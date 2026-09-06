package com.neepo.backend.service;

import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DisruptionJdbcRepository;
import com.neepo.backend.repository.RecoveryQueryJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.care.detection.DetectedDisruption;
import com.neepo.care.orchestration.RecoveryDecisionEngine;
import com.neepo.contracts.DisruptionEvent;
import com.neepo.contracts.DisruptionType;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.TripContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecoveryPlanGenerationService {
    private final TripRepository trips;
    private final DisruptionJdbcRepository disruptions;
    private final RecoveryQueryJdbcRepository recovery;
    private final TripContextSnapshotService contexts;
    private final RecoveryDecisionEngine decisionEngine;
    private final RecoveryDecisionPersistenceService persistence;

    public RecoveryPlanGenerationService(
            TripRepository trips,
            DisruptionJdbcRepository disruptions,
            RecoveryQueryJdbcRepository recovery,
            TripContextSnapshotService contexts,
            RecoveryDecisionEngine decisionEngine,
            RecoveryDecisionPersistenceService persistence
    ) {
        this.trips = Objects.requireNonNull(trips, "Trip repository is required");
        this.disruptions = Objects.requireNonNull(disruptions, "Disruption repository is required");
        this.recovery = Objects.requireNonNull(recovery, "Recovery query repository is required");
        this.contexts = Objects.requireNonNull(contexts, "Context snapshot service is required");
        this.decisionEngine = Objects.requireNonNull(decisionEngine, "Recovery decision engine is required");
        this.persistence = Objects.requireNonNull(persistence, "Recovery persistence service is required");
    }

    @Transactional
    public List<RecoveryPlan> generate(UUID companyId, UUID tripId, UUID traceId) {
        Objects.requireNonNull(companyId, "Company id is required");
        Objects.requireNonNull(tripId, "Trip id is required");
        Objects.requireNonNull(traceId, "Trace id is required");
        Trip trip = trips.findByIdAndCompanyId(tripId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found"));
        if (!trip.acceptsDisruption()) {
            throw conflict("TRIP_NOT_ACTIVE", "Recovery plans require an active trip");
        }

        List<RecoveryPlan> existing = recovery.findPlans(companyId, tripId);
        if (!existing.isEmpty()) {
            return existing;
        }

        DisruptionEvent disruption = disruptions.findActiveByTripId(companyId, tripId)
                .orElseThrow(() -> conflict(
                        "ACTIVE_DISRUPTION_NOT_FOUND", "An active disruption is required for plan generation"
                ));
        if (disruption.type() != DisruptionType.VEHICLE_OVERHEATING) {
            throw conflict(
                    "DISRUPTION_NOT_SUPPORTED",
                    "Recovery plan generation currently supports confirmed vehicle overheating"
            );
        }

        TripContext context = contexts.load(companyId, tripId, disruption.contextSnapshotId());
        var decision = decisionEngine.decide(context, disruption.id(), new DetectedDisruption(
                disruption.type(), disruption.severity(), disruption.detectedAt(),
                disruption.explanationCodes(), disruption.evidence()
        ));
        persistence.persist(
                companyId, disruption.contextSnapshotId(), disruption.id(), traceId, decision
        );
        return decision.selection().plans();
    }

    private static ResourceConflictException conflict(String code, String message) {
        return new ResourceConflictException(code, message);
    }
}
