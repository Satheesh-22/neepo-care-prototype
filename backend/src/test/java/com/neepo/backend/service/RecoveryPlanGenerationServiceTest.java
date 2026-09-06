package com.neepo.backend.service;

import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DisruptionJdbcRepository;
import com.neepo.backend.repository.RecoveryQueryJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.care.detection.DetectedDisruption;
import com.neepo.care.orchestration.RecoveryDecisionEngine;
import com.neepo.care.orchestration.RecoveryDecisionResult;
import com.neepo.care.selection.RecoveryPlanSelectionResult;
import com.neepo.contracts.DisruptionEvent;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.TripContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecoveryPlanGenerationServiceTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID DISRUPTION_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID SNAPSHOT_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID EVENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID TRACE_ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final Instant NOW = Instant.parse("2026-08-24T13:00:00Z");

    private TripRepository trips;
    private DisruptionJdbcRepository disruptions;
    private RecoveryQueryJdbcRepository recovery;
    private TripContextSnapshotService contexts;
    private RecoveryDecisionEngine engine;
    private RecoveryDecisionPersistenceService persistence;
    private RecoveryPlanGenerationService service;

    @BeforeEach
    void setUp() {
        trips = mock(TripRepository.class);
        disruptions = mock(DisruptionJdbcRepository.class);
        recovery = mock(RecoveryQueryJdbcRepository.class);
        contexts = mock(TripContextSnapshotService.class);
        engine = mock(RecoveryDecisionEngine.class);
        persistence = mock(RecoveryDecisionPersistenceService.class);
        service = new RecoveryPlanGenerationService(
                trips, disruptions, recovery, contexts, engine, persistence
        );
    }

    @Test
    void existingPlansMakeGenerationReplayIdempotent() {
        RecoveryPlan existing = mock(RecoveryPlan.class);
        prepareActiveTrip();
        when(recovery.findPlans(COMPANY_ID, TRIP_ID)).thenReturn(List.of(existing));

        assertEquals(List.of(existing), service.generate(COMPANY_ID, TRIP_ID, TRACE_ID));

        verifyNoInteractions(disruptions, contexts, engine, persistence);
    }

    @Test
    void executesCareDecisionAgainstReferencedSnapshotAndPersistsResult() {
        prepareActiveTrip();
        DisruptionEvent disruption = disruption(DisruptionType.VEHICLE_OVERHEATING);
        TripContext context = mock(TripContext.class);
        RecoveryDecisionResult decision = mock(RecoveryDecisionResult.class);
        RecoveryPlanSelectionResult selection = mock(RecoveryPlanSelectionResult.class);
        RecoveryPlan plan = mock(RecoveryPlan.class);
        when(recovery.findPlans(COMPANY_ID, TRIP_ID)).thenReturn(List.of());
        when(disruptions.findActiveByTripId(COMPANY_ID, TRIP_ID)).thenReturn(Optional.of(disruption));
        when(contexts.load(COMPANY_ID, TRIP_ID, SNAPSHOT_ID)).thenReturn(context);
        when(engine.decide(eq(context), eq(DISRUPTION_ID), any(DetectedDisruption.class)))
                .thenReturn(decision);
        when(decision.selection()).thenReturn(selection);
        when(selection.plans()).thenReturn(List.of(plan));

        assertEquals(List.of(plan), service.generate(COMPANY_ID, TRIP_ID, TRACE_ID));

        ArgumentCaptor<DetectedDisruption> detected = ArgumentCaptor.forClass(DetectedDisruption.class);
        verify(engine).decide(eq(context), eq(DISRUPTION_ID), detected.capture());
        assertEquals(DisruptionType.VEHICLE_OVERHEATING, detected.getValue().type());
        assertEquals("a".repeat(64), detected.getValue().evidence().get("contextHash"));
        verify(persistence).persist(COMPANY_ID, SNAPSHOT_ID, DISRUPTION_ID, TRACE_ID, decision);
    }

    @Test
    void missingTenantTripCannotInspectRecoveryData() {
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.generate(COMPANY_ID, TRIP_ID, TRACE_ID));

        verifyNoInteractions(disruptions, recovery, contexts, engine, persistence);
    }

    @Test
    void activeDisruptionIsRequired() {
        prepareActiveTrip();
        when(recovery.findPlans(COMPANY_ID, TRIP_ID)).thenReturn(List.of());
        when(disruptions.findActiveByTripId(COMPANY_ID, TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceConflictException.class, () ->
                service.generate(COMPANY_ID, TRIP_ID, TRACE_ID));

        verifyNoInteractions(contexts, engine, persistence);
    }

    @Test
    void unsupportedDisruptionFailsBeforeContextOrDecisionAccess() {
        prepareActiveTrip();
        when(recovery.findPlans(COMPANY_ID, TRIP_ID)).thenReturn(List.of());
        when(disruptions.findActiveByTripId(COMPANY_ID, TRIP_ID))
                .thenReturn(Optional.of(disruption(DisruptionType.ROUTE_BLOCKED)));

        assertThrows(ResourceConflictException.class, () ->
                service.generate(COMPANY_ID, TRIP_ID, TRACE_ID));

        verifyNoInteractions(contexts, engine, persistence);
    }

    private void prepareActiveTrip() {
        Trip trip = mock(Trip.class);
        when(trip.acceptsDisruption()).thenReturn(true);
        when(trips.findByIdAndCompanyId(TRIP_ID, COMPANY_ID)).thenReturn(Optional.of(trip));
    }

    private DisruptionEvent disruption(DisruptionType type) {
        return new DisruptionEvent(
                DISRUPTION_ID, EVENT_ID, TRIP_ID, SNAPSHOT_ID, type,
                DisruptionSeverity.HIGH, NOW, true, List.of("VEHICLE_TEMPERATURE_CRITICAL"),
                Map.of("contextHash", "a".repeat(64))
        );
    }
}
