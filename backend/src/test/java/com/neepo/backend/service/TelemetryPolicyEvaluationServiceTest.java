package com.neepo.backend.service;

import com.neepo.backend.dto.TelemetryPolicyRequest;
import com.neepo.backend.dto.TripContextSnapshotRecord;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DisruptionJdbcRepository;
import com.neepo.backend.repository.RecoveryQueryJdbcRepository;
import com.neepo.backend.repository.TripContextSnapshotJdbcRepository;
import com.neepo.care.telemetry.AdaptiveTelemetryPolicyEvaluator;
import com.neepo.care.telemetry.TelemetryPolicyDecision;
import com.neepo.care.telemetry.TelemetryPolicySignal;
import com.neepo.contracts.DisruptionEvent;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import com.neepo.contracts.NetworkState;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.TelemetryPolicyState;
import com.neepo.contracts.TripContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TelemetryPolicyEvaluationServiceTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID SNAPSHOT_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID DISRUPTION_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID EVENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID TRACE_ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    private TripContextSnapshotJdbcRepository snapshotRepository;
    private TripContextSnapshotService snapshots;
    private DisruptionJdbcRepository disruptions;
    private RecoveryQueryJdbcRepository recovery;
    private AdaptiveTelemetryPolicyEvaluator evaluator;
    private TelemetryPolicyPersistenceService persistence;
    private TelemetryPolicyEvaluationService service;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(TripContextSnapshotJdbcRepository.class);
        snapshots = mock(TripContextSnapshotService.class);
        disruptions = mock(DisruptionJdbcRepository.class);
        recovery = mock(RecoveryQueryJdbcRepository.class);
        evaluator = mock(AdaptiveTelemetryPolicyEvaluator.class);
        persistence = mock(TelemetryPolicyPersistenceService.class);
        service = new TelemetryPolicyEvaluationService(
                snapshotRepository, snapshots, disruptions, recovery, evaluator, persistence
        );
    }

    @Test
    void enrichesLatestContextAndPersistsSafelyParkedDecision() {
        TripContext previous = mock(TripContext.class);
        RecoveryPlan selected = mock(RecoveryPlan.class);
        when(selected.status()).thenReturn(RecoveryPlanStatus.SELECTED);
        when(previous.evaluationTime()).thenReturn(NOW.minusSeconds(30));
        when(previous.contextHash()).thenReturn("a".repeat(64));
        when(previous.networkState()).thenReturn(NetworkState.ONLINE);
        TripContextSnapshotRecord snapshot = snapshot(SNAPSHOT_ID, NOW.minusSeconds(30));
        when(snapshotRepository.findLatest(COMPANY_ID, TRIP_ID)).thenReturn(Optional.of(snapshot));
        when(snapshots.load(COMPANY_ID, TRIP_ID, SNAPSHOT_ID)).thenReturn(previous);
        when(disruptions.findActiveByTripId(COMPANY_ID, TRIP_ID)).thenReturn(Optional.of(disruption()));
        when(recovery.findPlans(COMPANY_ID, TRIP_ID)).thenReturn(List.of(selected));
        when(snapshots.persist(eq(COMPANY_ID), eq(TRIP_ID), any())).thenReturn(snapshot(UUID.randomUUID(), NOW));
        TelemetryPolicyDecision decision = new TelemetryPolicyDecision(
                TRIP_ID, EVENT_ID, TelemetryPolicyState.SAFELY_PARKED, 120, NOW, "SAFELY_PARKED"
        );
        when(evaluator.evaluate(any(), any())).thenReturn(decision);
        TelemetryPolicyRequest request = new TelemetryPolicyRequest(EVENT_ID, NOW, false, true);

        assertThat(service.evaluate(COMPANY_ID, TRIP_ID, TRACE_ID, request)).isEqualTo(decision);

        ArgumentCaptor<TripContext> context = ArgumentCaptor.forClass(TripContext.class);
        ArgumentCaptor<TelemetryPolicySignal> signal = ArgumentCaptor.forClass(TelemetryPolicySignal.class);
        verify(evaluator).evaluate(context.capture(), signal.capture());
        assertThat(context.getValue().activeDisruptions()).hasSize(1);
        assertThat(context.getValue().activeDisruptions().getFirst().disruptionId()).isEqualTo(DISRUPTION_ID);
        assertThat(context.getValue().currentRecoveryPlan()).isSameAs(selected);
        assertThat(context.getValue().evaluationTime()).isEqualTo(NOW);
        assertThat(context.getValue().contextHash()).hasSize(64).isNotEqualTo(previous.contextHash());
        assertThat(signal.getValue().safelyParked()).isTrue();
        verify(persistence).persist(COMPANY_ID, TRACE_ID, decision);
    }

    @Test
    void requiresAnExistingTelemetryContext() {
        when(snapshotRepository.findLatest(COMPANY_ID, TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.evaluate(
                COMPANY_ID, TRIP_ID, TRACE_ID,
                new TelemetryPolicyRequest(EVENT_ID, NOW, false, false)
        ));

        verifyNoInteractions(snapshots, disruptions, recovery, evaluator, persistence);
    }

    private TripContextSnapshotRecord snapshot(UUID id, Instant capturedAt) {
        return new TripContextSnapshotRecord(
                id, TRIP_ID, 1, capturedAt, BigDecimal.ONE, false,
                NetworkState.ONLINE, "{}", "a".repeat(64)
        );
    }

    private DisruptionEvent disruption() {
        return new DisruptionEvent(
                DISRUPTION_ID, UUID.randomUUID(), TRIP_ID, SNAPSHOT_ID,
                DisruptionType.VEHICLE_OVERHEATING, DisruptionSeverity.HIGH,
                NOW.minusSeconds(20), true, List.of("ENGINE_TEMPERATURE_LIMIT_EXCEEDED"), Map.of()
        );
    }
}
