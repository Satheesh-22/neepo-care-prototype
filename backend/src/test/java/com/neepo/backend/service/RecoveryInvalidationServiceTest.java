package com.neepo.backend.service;

import com.neepo.backend.dto.InvalidateRecoveryPlanRequest;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.RecoveryQueryJdbcRepository;
import com.neepo.care.invalidation.RecoveryPlanInvalidationResult;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecoveryInvalidationServiceTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID DISRUPTION_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID PLAN_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID EVENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID TRACE_ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final Instant SELECTED_AT = Instant.parse("2026-08-24T13:00:00Z");

    private RecoveryQueryJdbcRepository recovery;
    private ReplanningLifecycleService replanning;
    private RecoveryReplanningOrchestrationService replanningOrchestration;
    private RecoveryInvalidationService service;

    @BeforeEach
    void setUp() {
        recovery = mock(RecoveryQueryJdbcRepository.class);
        replanning = mock(ReplanningLifecycleService.class);
        replanningOrchestration = mock(RecoveryReplanningOrchestrationService.class);
        service = new RecoveryInvalidationService(recovery, replanning, replanningOrchestration);
    }

    @Test
    void invalidatesSelectedPlanAndStartsReplanning() {
        when(recovery.findPlan(COMPANY_ID, PLAN_ID)).thenReturn(Optional.of(plan(RecoveryPlanStatus.SELECTED)));

        RecoveryPlan invalidated = service.invalidate(COMPANY_ID, TRACE_ID, PLAN_ID, request());

        assertEquals(RecoveryPlanStatus.INVALIDATED, invalidated.status());
        ArgumentCaptor<RecoveryPlanInvalidationResult> result =
                ArgumentCaptor.forClass(RecoveryPlanInvalidationResult.class);
        verify(replanning).start(org.mockito.ArgumentMatchers.eq(COMPANY_ID),
                org.mockito.ArgumentMatchers.eq(TRACE_ID), result.capture());
        verify(replanningOrchestration).complete(
                org.mockito.ArgumentMatchers.eq(COMPANY_ID),
                org.mockito.ArgumentMatchers.eq(TRACE_ID),
                org.mockito.ArgumentMatchers.eq(result.getValue()),
                org.mockito.ArgumentMatchers.isNull()
        );
        assertEquals(EVENT_ID, result.getValue().invalidation().eventId());
        assertEquals("ROUTE_BLOCKED", result.getValue().invalidation().explanationCode());
    }

    @Test
    void replayCanRecreateDeterministicInvalidationFromInvalidatedPlan() {
        when(recovery.findPlan(COMPANY_ID, PLAN_ID)).thenReturn(Optional.of(plan(RecoveryPlanStatus.INVALIDATED)));

        RecoveryPlan invalidated = service.invalidate(COMPANY_ID, TRACE_ID, PLAN_ID, request());

        assertEquals(RecoveryPlanStatus.INVALIDATED, invalidated.status());
        verify(replanning).start(
                org.mockito.ArgumentMatchers.eq(COMPANY_ID),
                org.mockito.ArgumentMatchers.eq(TRACE_ID),
                org.mockito.ArgumentMatchers.any(RecoveryPlanInvalidationResult.class)
        );
        verify(replanningOrchestration).complete(
                org.mockito.ArgumentMatchers.eq(COMPANY_ID),
                org.mockito.ArgumentMatchers.eq(TRACE_ID),
                org.mockito.ArgumentMatchers.any(RecoveryPlanInvalidationResult.class),
                org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void missingTenantPlanCannotStartReplanning() {
        when(recovery.findPlan(COMPANY_ID, PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.invalidate(COMPANY_ID, TRACE_ID, PLAN_ID, request()));

        verifyNoInteractions(replanning);
        verifyNoInteractions(replanningOrchestration);
    }

    @Test
    void nonActivePlanHasConflictOutcome() {
        when(recovery.findPlan(COMPANY_ID, PLAN_ID)).thenReturn(Optional.of(plan(RecoveryPlanStatus.FEASIBLE)));

        assertThrows(ResourceConflictException.class, () ->
                service.invalidate(COMPANY_ID, TRACE_ID, PLAN_ID, request()));

        verifyNoInteractions(replanning);
        verifyNoInteractions(replanningOrchestration);
    }

    @Test
    void unsupportedExplanationCodeIsRejectedBeforePersistence() {
        when(recovery.findPlan(COMPANY_ID, PLAN_ID)).thenReturn(Optional.of(plan(RecoveryPlanStatus.SELECTED)));
        InvalidateRecoveryPlanRequest unsupported = new InvalidateRecoveryPlanRequest(
                EVENT_ID, "UNKNOWN_REASON", SELECTED_AT.plusSeconds(60), "route update"
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.invalidate(COMPANY_ID, TRACE_ID, PLAN_ID, unsupported));

        verifyNoInteractions(replanning);
        verifyNoInteractions(replanningOrchestration);
    }

    private InvalidateRecoveryPlanRequest request() {
        return new InvalidateRecoveryPlanRequest(
                EVENT_ID, "ROUTE_BLOCKED", SELECTED_AT.plusSeconds(60), "route update"
        );
    }

    private RecoveryPlan plan(RecoveryPlanStatus status) {
        return new RecoveryPlan(
                PLAN_ID, TRIP_ID, DISRUPTION_ID, RecoveryPlanType.EMERGENCY_ESCALATION,
                status, null, null, List.of(), evaluation(),
                SELECTED_AT.minusSeconds(30), SELECTED_AT
        );
    }

    private PlanEvaluation evaluation() {
        return new PlanEvaluation(
                PLAN_ID, true, List.of(), List.of(), Map.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("80.0000"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("80.0000"),
                0.95, Map.of("vehicle.health", "FRESH"), List.of(),
                SELECTED_AT.minusSeconds(30), "plan-evaluator-1.0.0", "a".repeat(64)
        );
    }
}
