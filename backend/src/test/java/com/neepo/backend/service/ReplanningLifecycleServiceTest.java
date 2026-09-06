package com.neepo.backend.service;

import com.neepo.backend.dto.RecoveryPlanLifecycleScope;
import com.neepo.backend.dto.ReplanningCycleRecord;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.ReplanningLifecycleJdbcRepository;
import com.neepo.care.invalidation.RecoveryPlanInvalidationResult;
import com.neepo.care.invalidation.ReplanningTrigger;
import com.neepo.care.orchestration.RecoveryDecisionResult;
import com.neepo.care.orchestration.RecoveryReplanningResult;
import com.neepo.care.selection.RecoveryPlanSelectionResult;
import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.PlanInvalidation;
import com.neepo.contracts.PlanInvalidationReason;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReplanningLifecycleServiceTest {
    private static final UUID COMPANY_ID = id("10000000-0000-0000-0000-000000000001");
    private static final UUID TRACE_ID = id("20000000-0000-0000-0000-000000000002");
    private static final UUID TRIP_ID = id("30000000-0000-0000-0000-000000000003");
    private static final UUID DISRUPTION_ID = id("40000000-0000-0000-0000-000000000004");
    private static final UUID PLAN_ID = id("50000000-0000-0000-0000-000000000005");
    private static final UUID EVENT_ID = id("60000000-0000-0000-0000-000000000006");
    private static final UUID INVALIDATION_ID = id("70000000-0000-0000-0000-000000000007");
    private static final UUID CYCLE_ID = id("80000000-0000-0000-0000-000000000008");
    private static final UUID CONTEXT_ID = id("90000000-0000-0000-0000-000000000009");
    private static final UUID NEW_PLAN_ID = id("a0000000-0000-0000-0000-00000000000a");
    private static final Instant NOW = Instant.parse("2026-08-24T11:00:00Z");

    private ReplanningLifecycleJdbcRepository repository;
    private RecoveryDecisionPersistenceService decisionPersistence;
    private ReplanningLifecycleService service;

    @BeforeEach
    void setUp() {
        repository = mock(ReplanningLifecycleJdbcRepository.class);
        decisionPersistence = mock(RecoveryDecisionPersistenceService.class);
        service = new ReplanningLifecycleService(repository, decisionPersistence);
    }

    @Test
    void startsCycleAfterPersistingInvalidationAndPlanState() {
        RecoveryPlanInvalidationResult result = invalidationResult();
        when(repository.lockPlan(COMPANY_ID, PLAN_ID)).thenReturn(Optional.of(scope(RecoveryPlanStatus.SELECTED)));
        when(repository.findInvalidation(EVENT_ID)).thenReturn(Optional.empty());
        when(repository.nextCycleNumber(DISRUPTION_ID)).thenReturn(2);
        when(repository.insertStartedCycle(
                any(UUID.class), eq(TRIP_ID), eq(DISRUPTION_ID), eq(INVALIDATION_ID), eq(2), eq(NOW)
        )).thenAnswer(invocation -> cycle(invocation.getArgument(0), "STARTED", null, null));

        ReplanningCycleRecord cycle = service.start(COMPANY_ID, TRACE_ID, result);

        assertEquals(2, cycle.cycleNumber());
        InOrder order = inOrder(repository);
        order.verify(repository).lockPlan(COMPANY_ID, PLAN_ID);
        order.verify(repository).findInvalidation(EVENT_ID);
        order.verify(repository).insertInvalidation(result.invalidation());
        order.verify(repository).invalidatePlan(PLAN_ID);
        order.verify(repository).insertAudit(
                TRACE_ID, "PLAN_INVALIDATED", "RECOVERY_PLAN", PLAN_ID, NOW, result.invalidation()
        );
        order.verify(repository).nextCycleNumber(DISRUPTION_ID);
        order.verify(repository).insertStartedCycle(
                any(UUID.class), eq(TRIP_ID), eq(DISRUPTION_ID), eq(INVALIDATION_ID), eq(2), eq(NOW)
        );
        order.verify(repository).insertAudit(
                TRACE_ID, "REPLANNING_STARTED", "REPLANNING_CYCLE", cycle.id(), NOW,
                result.replanningTrigger()
        );
    }

    @Test
    void exactInvalidationReplayReturnsExistingCycleWithoutWriting() {
        RecoveryPlanInvalidationResult result = invalidationResult();
        ReplanningCycleRecord existing = cycle(CYCLE_ID, "STARTED", null, null);
        when(repository.lockPlan(COMPANY_ID, PLAN_ID)).thenReturn(Optional.of(scope(RecoveryPlanStatus.INVALIDATED)));
        when(repository.findInvalidation(EVENT_ID)).thenReturn(Optional.of(result.invalidation()));
        when(repository.findCycleByInvalidation(INVALIDATION_ID)).thenReturn(Optional.of(existing));

        assertEquals(existing, service.start(COMPANY_ID, TRACE_ID, result));

        verify(repository, never()).insertInvalidation(any());
        verify(repository, never()).invalidatePlan(any());
    }

    @Test
    void differentInvalidationCannotReplaceAnInvalidatedPlan() {
        RecoveryPlanInvalidationResult result = invalidationResult();
        PlanInvalidation other = new PlanInvalidation(
                INVALIDATION_ID, EVENT_ID, PLAN_ID, PlanInvalidationReason.ROUTE_BLOCKED,
                "ROUTE_BLOCKED", NOW, "different"
        );
        when(repository.lockPlan(COMPANY_ID, PLAN_ID)).thenReturn(Optional.of(scope(RecoveryPlanStatus.INVALIDATED)));
        when(repository.findInvalidation(EVENT_ID)).thenReturn(Optional.of(other));

        assertThrows(ResourceConflictException.class, () -> service.start(COMPANY_ID, TRACE_ID, result));
    }

    @Test
    void missingTenantPlanCannotStartReplanning() {
        when(repository.lockPlan(COMPANY_ID, PLAN_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.start(COMPANY_ID, TRACE_ID, invalidationResult()));

        verify(repository, never()).insertInvalidation(any());
    }

    @Test
    void completionPersistsDecisionBeforeCompletingAndAuditingCycle() {
        ReplanningCycleRecord started = cycle(CYCLE_ID, "STARTED", null, null);
        RecoveryReplanningResult result = replanningResult();
        when(repository.lockCycle(COMPANY_ID, CYCLE_ID)).thenReturn(Optional.of(started));

        ReplanningCycleRecord completed = service.complete(
                COMPANY_ID, CONTEXT_ID, TRACE_ID, CYCLE_ID, result
        );

        assertEquals("COMPLETED", completed.status());
        assertEquals(NEW_PLAN_ID, completed.selectedPlanId());
        InOrder order = inOrder(repository, decisionPersistence);
        order.verify(repository).lockCycle(COMPANY_ID, CYCLE_ID);
        order.verify(decisionPersistence).persist(
                COMPANY_ID, CONTEXT_ID, DISRUPTION_ID, TRACE_ID, result.decision()
        );
        order.verify(repository).completeCycle(CYCLE_ID, NOW.plusSeconds(60), NEW_PLAN_ID);
        order.verify(repository).insertAudit(
                TRACE_ID, "REPLANNING_COMPLETED", "REPLANNING_CYCLE", CYCLE_ID,
                NOW.plusSeconds(60), result.decision().selection().explanation()
        );
    }

    @Test
    void completionReplayReturnsExistingCycleWithoutRewritingDecision() {
        ReplanningCycleRecord completed = cycle(
                CYCLE_ID, "COMPLETED", NOW.plusSeconds(60), NEW_PLAN_ID
        );
        when(repository.lockCycle(COMPANY_ID, CYCLE_ID)).thenReturn(Optional.of(completed));

        assertEquals(completed, service.complete(
                COMPANY_ID, CONTEXT_ID, TRACE_ID, CYCLE_ID, replanningResult()
        ));

        verify(decisionPersistence, never()).persist(any(), any(), any(), any(), any());
    }

    @Test
    void failureIsTerminalAndAudited() {
        ReplanningCycleRecord started = cycle(CYCLE_ID, "STARTED", null, null);
        Instant failedAt = NOW.plusSeconds(30);
        when(repository.lockCycle(COMPANY_ID, CYCLE_ID)).thenReturn(Optional.of(started));

        ReplanningCycleRecord failed = service.fail(
                COMPANY_ID, TRACE_ID, CYCLE_ID, failedAt, "No feasible context"
        );

        assertEquals("FAILED", failed.status());
        verify(repository).failCycle(CYCLE_ID, failedAt);
        verify(repository).insertAudit(
                TRACE_ID, "REPLANNING_FAILED", "REPLANNING_CYCLE", CYCLE_ID,
                failedAt, Map.of("reason", "No feasible context")
        );
    }

    private RecoveryPlanInvalidationResult invalidationResult() {
        RecoveryPlan invalidated = plan(PLAN_ID, RecoveryPlanStatus.INVALIDATED, NOW);
        PlanInvalidation invalidation = new PlanInvalidation(
                INVALIDATION_ID, EVENT_ID, PLAN_ID, PlanInvalidationReason.PROVIDER_CANCELLED,
                "PROVIDER_CANCELLED", NOW, "Provider cancelled"
        );
        ReplanningTrigger trigger = new ReplanningTrigger(
                TRIP_ID, DISRUPTION_ID, INVALIDATION_ID, EVENT_ID, NOW
        );
        return new RecoveryPlanInvalidationResult(invalidated, invalidation, trigger);
    }

    private RecoveryReplanningResult replanningResult() {
        Instant completedAt = NOW.plusSeconds(60);
        RecoveryPlan selected = plan(NEW_PLAN_ID, RecoveryPlanStatus.SELECTED, completedAt);
        DecisionExplanation explanation = new DecisionExplanation(
                TRIP_ID, DISRUPTION_ID, NEW_PLAN_ID, List.of(),
                List.of("PLAN_SELECTED_HIGHEST_UTILITY"), Map.of(),
                "FINAL_UTILITY_DESC_THEN_PLAN_UUID_ASC", completedAt
        );
        RecoveryDecisionResult decision = mock(RecoveryDecisionResult.class);
        when(decision.selection()).thenReturn(new RecoveryPlanSelectionResult(
                List.of(selected), selected, explanation
        ));
        return new RecoveryReplanningResult(
                new ReplanningTrigger(TRIP_ID, DISRUPTION_ID, INVALIDATION_ID, EVENT_ID, NOW),
                decision
        );
    }

    private RecoveryPlan plan(UUID id, RecoveryPlanStatus status, Instant selectedAt) {
        PlanEvaluation evaluation = new PlanEvaluation(
                id, true, List.of(), List.of(), Map.of(),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                0.95, Map.of("vehicle.health", "FRESH"), List.of(),
                selectedAt, "plan-evaluator-1.0.0", "a".repeat(64)
        );
        return new RecoveryPlan(
                id, TRIP_ID, DISRUPTION_ID, RecoveryPlanType.EMERGENCY_ESCALATION,
                status, null, null, List.of(), evaluation,
                NOW.minusSeconds(60), selectedAt
        );
    }

    private RecoveryPlanLifecycleScope scope(RecoveryPlanStatus status) {
        return new RecoveryPlanLifecycleScope(PLAN_ID, TRIP_ID, DISRUPTION_ID, status);
    }

    private ReplanningCycleRecord cycle(
            UUID id, String status, Instant completedAt, UUID selectedPlanId
    ) {
        return new ReplanningCycleRecord(
                id, TRIP_ID, DISRUPTION_ID, INVALIDATION_ID, 2,
                status, NOW, completedAt, selectedPlanId
        );
    }

    private static UUID id(String value) {
        return UUID.fromString(value);
    }
}
