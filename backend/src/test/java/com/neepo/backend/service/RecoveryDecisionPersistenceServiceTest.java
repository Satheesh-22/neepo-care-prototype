package com.neepo.backend.service;

import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.RecoveryDecisionJdbcRepository;
import com.neepo.care.orchestration.RecoveryDecisionResult;
import com.neepo.care.selection.RecoveryPlanSelectionResult;
import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanStep;
import com.neepo.contracts.RecoveryPlanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecoveryDecisionPersistenceServiceTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TRIP_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID CONTEXT_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private static final UUID DISRUPTION_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
    private static final UUID SELECTED_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID REJECTED_ID = UUID.fromString("60000000-0000-0000-0000-000000000006");
    private static final UUID TRACE_ID = UUID.fromString("70000000-0000-0000-0000-000000000007");
    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

    private RecoveryDecisionJdbcRepository repository;
    private RecoveryDecisionPersistenceService service;

    @BeforeEach
    void setUp() {
        repository = mock(RecoveryDecisionJdbcRepository.class);
        service = new RecoveryDecisionPersistenceService(repository);
    }

    @Test
    void locksScopeAndPersistsCompleteDecisionInStableOrder() {
        RecoveryPlan selected = plan(SELECTED_ID, DISRUPTION_ID, RecoveryPlanStatus.SELECTED, true);
        RecoveryPlan rejected = plan(REJECTED_ID, DISRUPTION_ID, RecoveryPlanStatus.REJECTED, false);
        DecisionExplanation explanation = explanation(DISRUPTION_ID, SELECTED_ID);
        RecoveryDecisionResult decision = decision(List.of(selected, rejected), selected, explanation);
        when(repository.lockScope(COMPANY_ID, TRIP_ID, CONTEXT_ID, DISRUPTION_ID)).thenReturn(true);
        when(repository.nextGenerationSequence(DISRUPTION_ID)).thenReturn(12);

        RecoveryPlan result = service.persist(COMPANY_ID, CONTEXT_ID, DISRUPTION_ID, TRACE_ID, decision);

        assertEquals(selected, result);
        InOrder order = inOrder(repository);
        order.verify(repository).lockScope(COMPANY_ID, TRIP_ID, CONTEXT_ID, DISRUPTION_ID);
        order.verify(repository).nextGenerationSequence(DISRUPTION_ID);
        order.verify(repository).insertPlan(selected, 12);
        order.verify(repository).insertPlan(rejected, 13);
        order.verify(repository).insertExplanationAudit(TRACE_ID, explanation);
    }

    @Test
    void missingTenantDecisionScopeCannotWritePartialDecision() {
        RecoveryPlan selected = plan(SELECTED_ID, DISRUPTION_ID, RecoveryPlanStatus.SELECTED, true);
        RecoveryDecisionResult decision = decision(
                List.of(selected), selected, selectedOnlyExplanation(DISRUPTION_ID, SELECTED_ID)
        );
        when(repository.lockScope(COMPANY_ID, TRIP_ID, CONTEXT_ID, DISRUPTION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                service.persist(COMPANY_ID, CONTEXT_ID, DISRUPTION_ID, TRACE_ID, decision));

        verify(repository, never()).nextGenerationSequence(DISRUPTION_ID);
        verify(repository, never()).insertPlan(selected, 1);
    }

    @Test
    void selectedPlanMustMatchPersistedDisruptionBeforeLocking() {
        UUID otherDisruption = UUID.fromString("80000000-0000-0000-0000-000000000008");
        RecoveryPlan selected = plan(SELECTED_ID, otherDisruption, RecoveryPlanStatus.SELECTED, true);
        RecoveryDecisionResult decision = decision(
                List.of(selected), selected, selectedOnlyExplanation(otherDisruption, SELECTED_ID)
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.persist(COMPANY_ID, CONTEXT_ID, DISRUPTION_ID, TRACE_ID, decision));

        verifyNoInteractions(repository);
    }

    @Test
    void explanationMustMatchSelectedPlanBeforeLocking() {
        RecoveryPlan selected = plan(SELECTED_ID, DISRUPTION_ID, RecoveryPlanStatus.SELECTED, true);
        RecoveryDecisionResult decision = decision(
                List.of(selected), selected, selectedOnlyExplanation(DISRUPTION_ID, REJECTED_ID)
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.persist(COMPANY_ID, CONTEXT_ID, DISRUPTION_ID, TRACE_ID, decision));

        verifyNoInteractions(repository);
    }

    @Test
    void duplicatePlanIdsAreRejectedBeforeLocking() {
        RecoveryPlan selected = plan(SELECTED_ID, DISRUPTION_ID, RecoveryPlanStatus.SELECTED, true);
        RecoveryDecisionResult decision = decision(
                List.of(selected, selected), selected, explanation(DISRUPTION_ID, SELECTED_ID)
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.persist(COMPANY_ID, CONTEXT_ID, DISRUPTION_ID, TRACE_ID, decision));

        verifyNoInteractions(repository);
    }

    private RecoveryDecisionResult decision(
            List<RecoveryPlan> plans,
            RecoveryPlan selected,
            DecisionExplanation explanation
    ) {
        RecoveryDecisionResult decision = mock(RecoveryDecisionResult.class);
        when(decision.selection()).thenReturn(new RecoveryPlanSelectionResult(plans, selected, explanation));
        return decision;
    }

    private RecoveryPlan plan(
            UUID id,
            UUID disruptionId,
            RecoveryPlanStatus status,
            boolean feasible
    ) {
        PlanEvaluation evaluation = new PlanEvaluation(
                id,
                feasible,
                feasible ? List.of() : List.of("VEHICLE-SAFETY-001"),
                List.of(),
                Map.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0.95,
                Map.of("vehicle.health", "FRESH"),
                feasible ? List.of("PLAN_SELECTED_HIGHEST_UTILITY") : List.of("VEHICLE_UNSAFE"),
                NOW,
                "plan-evaluator-1.0.0",
                "a".repeat(64)
        );
        RecoveryPlanStep step = new RecoveryPlanStep(
                1,
                "CONTACT_PROVIDER",
                "Contact the assigned recovery provider",
                null,
                NOW,
                NOW.plusSeconds(600),
                List.of("phone")
        );
        return new RecoveryPlan(
                id,
                TRIP_ID,
                disruptionId,
                feasible ? RecoveryPlanType.EMERGENCY_ESCALATION : RecoveryPlanType.WORKSHOP_REPAIR,
                status,
                null,
                null,
                List.of(step),
                evaluation,
                NOW.minusSeconds(1),
                status == RecoveryPlanStatus.SELECTED ? NOW : null
        );
    }

    private DecisionExplanation explanation(UUID disruptionId, UUID selectedPlanId) {
        return new DecisionExplanation(
                TRIP_ID,
                disruptionId,
                selectedPlanId,
                List.of(REJECTED_ID),
                List.of("PLAN_SELECTED_HIGHEST_UTILITY"),
                Map.of(REJECTED_ID, List.of("VEHICLE_UNSAFE")),
                "FINAL_UTILITY_DESC_THEN_PLAN_UUID_ASC",
                NOW
        );
    }

    private DecisionExplanation selectedOnlyExplanation(UUID disruptionId, UUID selectedPlanId) {
        return new DecisionExplanation(
                TRIP_ID,
                disruptionId,
                selectedPlanId,
                List.of(),
                List.of("PLAN_SELECTED_HIGHEST_UTILITY"),
                Map.of(),
                "FINAL_UTILITY_DESC_THEN_PLAN_UUID_ASC",
                NOW
        );
    }
}
