package com.neepo.backend.service;

import com.neepo.backend.dto.CreateDisruptionRequest;
import com.neepo.backend.repository.DisruptionJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.care.detection.DisruptionDetector;
import com.neepo.contracts.DisruptionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class TelemetryCareProcessingService {
    private final TripRepository trips;
    private final TripContextAssemblyService contexts;
    private final TripContextSnapshotService snapshots;
    private final DisruptionDetector detector;
    private final DisruptionJdbcRepository disruptionRepository;
    private final DisruptionService disruptions;
    private final RecoveryPlanGenerationService generation;

    public TelemetryCareProcessingService(
            TripRepository trips,
            TripContextAssemblyService contexts,
            TripContextSnapshotService snapshots,
            DisruptionDetector detector,
            DisruptionJdbcRepository disruptionRepository,
            DisruptionService disruptions,
            RecoveryPlanGenerationService generation
    ) {
        this.trips = trips;
        this.contexts = contexts;
        this.snapshots = snapshots;
        this.detector = detector;
        this.disruptionRepository = disruptionRepository;
        this.disruptions = disruptions;
        this.generation = generation;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(TelemetryUpdatedEvent event) {
        var trip = trips.findByIdAndCompanyId(event.observation().tripId(), event.companyId());
        if (trip.isEmpty() || !trip.get().acceptsDisruption()) {
            return;
        }
        var context = contexts.assemble(event.companyId(), event.observation());
        var snapshot = snapshots.persist(event.companyId(), event.observation().tripId(), context);
        if (disruptionRepository.findActiveByTripId(event.companyId(), event.observation().tripId()).isPresent()) {
            return;
        }

        detector.detect(context).disruptions().stream()
                .filter(disruption -> disruption.type() == DisruptionType.VEHICLE_OVERHEATING)
                .findFirst()
                .ifPresent(disruption -> {
                    UUID disruptionEventId = deterministicEventId(event.eventId(), disruption.type());
                    disruptions.report(event.companyId(), new CreateDisruptionRequest(
                            disruptionEventId,
                            event.observation().tripId(),
                            snapshot.id(),
                            disruption.type(),
                            disruption.severity(),
                            disruption.detectedAt(),
                            disruption.explanationCodes(),
                            disruption.evidence()
                    ));
                    generation.generate(event.companyId(), event.observation().tripId(), event.eventId());
                });
    }

    private static UUID deterministicEventId(UUID telemetryEventId, DisruptionType type) {
        String value = "care-disruption:" + telemetryEventId + ":" + type;
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
