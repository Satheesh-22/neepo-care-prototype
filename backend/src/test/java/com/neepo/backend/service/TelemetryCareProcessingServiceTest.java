package com.neepo.backend.service;

import com.neepo.backend.domain.NetworkState;
import com.neepo.backend.domain.ObservationStatus;
import com.neepo.backend.domain.TelemetrySource;
import com.neepo.backend.dto.CreateDisruptionRequest;
import com.neepo.backend.dto.GeoPointRequest;
import com.neepo.backend.dto.TelemetryObservationRequest;
import com.neepo.backend.dto.TripContextSnapshotRecord;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.repository.DisruptionJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.care.context.ContextFreshnessResult;
import com.neepo.care.detection.DetectedDisruption;
import com.neepo.care.detection.DisruptionDetectionResult;
import com.neepo.care.detection.DisruptionDetector;
import com.neepo.contracts.DisruptionEvent;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import com.neepo.contracts.TripContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryCareProcessingServiceTest {
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("00000000-0000-4000-8600-000000000001");
    private static final UUID VEHICLE_ID = UUID.fromString("00000000-0000-4000-8300-000000000001");
    private static final UUID SNAPSHOT_ID = UUID.fromString("00000000-0000-4000-8900-000000000001");
    private static final UUID TELEMETRY_EVENT_ID = UUID.fromString("00000000-0000-4000-9000-000000000001");
    private static final UUID TRACE_ID = UUID.fromString("00000000-0000-4000-9100-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-24T16:30:00Z");

    @Test
    void startsANewTransactionWhenInvokedAfterTelemetryCommit() throws Exception {
        Transactional transaction = TelemetryCareProcessingService.class
                .getMethod("process", TelemetryUpdatedEvent.class)
                .getAnnotation(Transactional.class);

        assertThat(transaction).isNotNull();
        assertThat(transaction.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void detectedOverheatingCreatesDisruptionAndGeneratesRecoveryPlans() {
        Fixture fixture = fixture();
        DetectedDisruption overheating = new DetectedDisruption(
                DisruptionType.VEHICLE_OVERHEATING,
                DisruptionSeverity.HIGH,
                NOW,
                List.of("ENGINE_TEMPERATURE_LIMIT_EXCEEDED"),
                Map.of("observedTemperatureCelsius", new BigDecimal("112"))
        );
        when(fixture.detector.detect(fixture.context)).thenReturn(new DisruptionDetectionResult(
                mock(ContextFreshnessResult.class), List.of(overheating), List.of()
        ));

        fixture.service.process(fixture.event);

        ArgumentCaptor<CreateDisruptionRequest> request = ArgumentCaptor.forClass(CreateDisruptionRequest.class);
        verify(fixture.disruptions).report(eq(COMPANY_ID), request.capture());
        assertThat(request.getValue().tripId()).isEqualTo(TRIP_ID);
        assertThat(request.getValue().contextSnapshotId()).isEqualTo(SNAPSHOT_ID);
        assertThat(request.getValue().type()).isEqualTo(DisruptionType.VEHICLE_OVERHEATING);
        verify(fixture.generation).generate(COMPANY_ID, TRIP_ID, TRACE_ID);
    }

    @Test
    void activeDisruptionPreventsDuplicateDetectionAndGeneration() {
        Fixture fixture = fixture();
        when(fixture.disruptionRepository.findActiveByTripId(COMPANY_ID, TRIP_ID))
                .thenReturn(Optional.of(mock(DisruptionEvent.class)));

        fixture.service.process(fixture.event);

        verify(fixture.detector, never()).detect(any());
        verify(fixture.disruptions, never()).report(any(), any());
        verify(fixture.generation, never()).generate(any(), any(), any());
    }

    @Test
    void inactiveTripSkipsCareProcessing() {
        Fixture fixture = fixture();
        Trip inactive = mock(Trip.class);
        when(fixture.trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(inactive));

        fixture.service.process(fixture.event);

        verify(fixture.contexts, never()).assemble(any(), any());
        verify(fixture.snapshots, never()).persist(any(), any(), any());
    }

    private Fixture fixture() {
        TripRepository trips = mock(TripRepository.class);
        TripContextAssemblyService contexts = mock(TripContextAssemblyService.class);
        TripContextSnapshotService snapshots = mock(TripContextSnapshotService.class);
        DisruptionDetector detector = mock(DisruptionDetector.class);
        DisruptionJdbcRepository disruptionRepository = mock(DisruptionJdbcRepository.class);
        DisruptionService disruptions = mock(DisruptionService.class);
        RecoveryPlanGenerationService generation = mock(RecoveryPlanGenerationService.class);
        TripContext context = mock(TripContext.class);
        TelemetryUpdatedEvent event = event();

        Trip trip = mock(Trip.class);
        when(trip.acceptsDisruption()).thenReturn(true);
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(trip));
        when(contexts.assemble(COMPANY_ID, event.observation())).thenReturn(context);
        when(snapshots.persist(COMPANY_ID, TRIP_ID, context)).thenReturn(new TripContextSnapshotRecord(
                SNAPSHOT_ID, TRIP_ID, 1, NOW, new BigDecimal("0.980"), false,
                com.neepo.contracts.NetworkState.ONLINE, "{}", "a".repeat(64)
        ));
        when(disruptionRepository.findActiveByTripId(COMPANY_ID, TRIP_ID)).thenReturn(Optional.empty());

        var service = new TelemetryCareProcessingService(
                trips, contexts, snapshots, detector, disruptionRepository, disruptions, generation
        );
        return new Fixture(
                service, trips, contexts, snapshots, detector, disruptionRepository,
                disruptions, generation, context, event
        );
    }

    private TelemetryUpdatedEvent event() {
        var observation = new TelemetryObservationRequest(
                TELEMETRY_EVENT_ID, TRIP_ID, VEHICLE_ID, NOW, NOW, NOW.plusSeconds(300),
                new GeoPointRequest(new BigDecimal("11.42"), new BigDecimal("77.69")),
                new BigDecimal("112"), new BigDecimal("5.2"), new BigDecimal("42"),
                new BigDecimal("0.98"), ObservationStatus.FRESH, TelemetrySource.SIMULATOR,
                NetworkState.ONLINE
        );
        return new TelemetryUpdatedEvent(TRACE_ID, COMPANY_ID, observation, NOW);
    }

    private record Fixture(
            TelemetryCareProcessingService service,
            TripRepository trips,
            TripContextAssemblyService contexts,
            TripContextSnapshotService snapshots,
            DisruptionDetector detector,
            DisruptionJdbcRepository disruptionRepository,
            DisruptionService disruptions,
            RecoveryPlanGenerationService generation,
            TripContext context,
            TelemetryUpdatedEvent event
    ) { }
}
