package com.neepo.care.selection;

import com.neepo.care.scoring.RecoveryPlanScorer;
import com.neepo.care.scoring.RecoveryPlanScoringResult;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanStep;
import com.neepo.contracts.RecoveryPlanType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryPlanSelectorTest {
    private static final Instant NOW = Instant.parse("2026-08-23T12:00:00Z");
    private static final UUID TRIP_ID = id("00000000-0000-4000-8600-000000000001");
    private static final UUID DISRUPTION_ID = id("00000000-0000-4000-9000-000000000001");
    private static final UUID PLAN_1 = id("00000000-0000-4000-9100-000000000001");
    private static final UUID PLAN_2 = id("00000000-0000-4000-9100-000000000002");
    private static final UUID PLAN_3 = id("00000000-0000-4000-9100-000000000003");
    private static final UUID PROVIDER_1 = id("00000000-0000-4000-8700-000000000001");
    private static final UUID PROVIDER_2 = id("00000000-0000-4000-8700-000000000002");

    private final RecoveryPlanSelector selector = new RecoveryPlanSelector();

    @Test
    void selectsHighestUtilityNormalPlanAndKeepsEmergencyAsFallback() {
        RecoveryPlan lower = normal(PLAN_1, PROVIDER_1, RecoveryPlanType.TOW_REPAIR_OR_TRANSFER, "70.0000");
        RecoveryPlan higher = normal(PLAN_2, PROVIDER_2, RecoveryPlanType.WORKSHOP_REPAIR, "80.0000");
        RecoveryPlan emergency = emergency(PLAN_3);

        RecoveryPlanSelectionResult result = selector.select(scored(lower, emergency, higher));

        assertEquals(PLAN_2, result.selectedPlan().id());
        assertEquals(RecoveryPlanStatus.SELECTED, result.selectedPlan().status());
        assertEquals(NOW, result.selectedPlan().selectedAt());
        assertEquals(List.of(RecoveryPlanSelector.PLAN_SELECTED_HIGHEST_UTILITY),
                result.explanation().selectionExplanationCodes());
        assertFalse(result.emergencyFallbackSelected());
        assertEquals(RecoveryPlanStatus.FEASIBLE, plan(result, PLAN_3).status());
    }

    @Test
    void exactUtilityTieUsesCanonicalUuidRegardlessOfDecimalScale() {
        RecoveryPlan canonicalFirst = normal(
                PLAN_1, PROVIDER_2, RecoveryPlanType.TOW_REPAIR_OR_TRANSFER, new BigDecimal("80.0")
        );
        RecoveryPlan canonicalSecond = normal(
                PLAN_2, PROVIDER_1, RecoveryPlanType.WORKSHOP_REPAIR, new BigDecimal("80.0000")
        );

        RecoveryPlanSelectionResult result = selector.select(scored(
                canonicalSecond, emergency(PLAN_3), canonicalFirst
        ));

        assertEquals(PLAN_1, result.selectedPlan().id());
        assertEquals(
                List.of(
                        RecoveryPlanSelector.PLAN_SELECTED_HIGHEST_UTILITY,
                        RecoveryPlanSelector.PLAN_SELECTED_TIE_BREAK
                ),
                result.explanation().selectionExplanationCodes()
        );
        assertEquals(RecoveryPlanSelector.DETERMINISTIC_TIE_BREAK_RULE,
                result.explanation().deterministicTieBreakRule());
    }

    @Test
    void rejectedPlanCannotWinEvenWithHigherUtility() {
        RecoveryPlan rejected = rejected(PLAN_1, "99.0000", "PROVIDER_UNAVAILABLE");
        RecoveryPlan feasible = normal(
                PLAN_2, PROVIDER_2, RecoveryPlanType.WORKSHOP_REPAIR, "30.0000"
        );

        RecoveryPlanSelectionResult result = selector.select(scored(
                rejected, emergency(PLAN_3), feasible
        ));

        assertEquals(PLAN_2, result.selectedPlan().id());
        assertEquals(List.of(PLAN_1), result.explanation().rejectedPlanIds());
        assertEquals(List.of("PROVIDER_UNAVAILABLE"),
                result.explanation().rejectionExplanationCodes().get(PLAN_1));
    }

    @Test
    void selectsEmergencyOnlyWhenEveryNormalPlanIsRejected() {
        RecoveryPlan rejected = rejected(PLAN_1, "0.0000", "DRIVER_HOURS_INSUFFICIENT");

        RecoveryPlanSelectionResult result = selector.select(scored(emergency(PLAN_3), rejected));

        assertEquals(PLAN_3, result.selectedPlan().id());
        assertTrue(result.emergencyFallbackSelected());
        assertEquals(
                List.of(
                        RecoveryPlanSelector.NO_FEASIBLE_RECOVERY_PLAN,
                        RecoveryPlanSelector.EMERGENCY_ESCALATION_SELECTED
                ),
                result.explanation().selectionExplanationCodes()
        );
    }

    @Test
    void refusesToInventFallbackWhenEmergencyCandidateIsMissing() {
        assertThrows(IllegalArgumentException.class, () -> selector.select(scored(
                rejected(PLAN_1, "0.0000", "PROVIDER_UNAVAILABLE")
        )));
    }

    @Test
    void refusesAmbiguousEmergencyFallbacks() {
        assertThrows(IllegalArgumentException.class, () -> selector.select(scored(
                emergency(PLAN_2), emergency(PLAN_3)
        )));
    }

    @Test
    void explanationOrdersOnlyHardRejectedPlansAndCodesByPlanId() {
        RecoveryPlan rejectedLast = rejected(PLAN_3, "0.0000", "HEIGHT_CLEARANCE_EXCEEDED");
        RecoveryPlan rejectedFirst = rejected(PLAN_1, "0.0000", "PROVIDER_UNAVAILABLE");
        RecoveryPlan selected = normal(
                PLAN_2, PROVIDER_1, RecoveryPlanType.WORKSHOP_REPAIR, "60.0000"
        );
        UUID emergencyId = id("00000000-0000-4000-9100-000000000004");

        RecoveryPlanSelectionResult result = selector.select(scored(
                rejectedLast, emergency(emergencyId), selected, rejectedFirst
        ));

        assertEquals(List.of(PLAN_1, PLAN_3), result.explanation().rejectedPlanIds());
        assertEquals(List.of(PLAN_1, PLAN_3),
                new ArrayList<>(result.explanation().rejectionExplanationCodes().keySet()));
        assertFalse(result.explanation().rejectedPlanIds().contains(emergencyId));
        assertThrows(UnsupportedOperationException.class, () ->
                result.explanation().rejectionExplanationCodes().put(PLAN_2, List.of()));
    }

    @Test
    void inputOrderCannotChangeSelectionOrReturnedOrdering() {
        List<RecoveryPlan> plans = new ArrayList<>(List.of(
                normal(PLAN_1, PROVIDER_1, RecoveryPlanType.TOW_REPAIR_OR_TRANSFER, "70.0000"),
                normal(PLAN_2, PROVIDER_2, RecoveryPlanType.WORKSHOP_REPAIR, "80.0000"),
                emergency(PLAN_3)
        ));
        RecoveryPlanSelectionResult forward = selector.select(new RecoveryPlanScoringResult(plans));
        Collections.reverse(plans);

        RecoveryPlanSelectionResult reversed = selector.select(new RecoveryPlanScoringResult(plans));

        assertEquals(forward, reversed);
    }

    @Test
    void selectionDoesNotMutateScoredPlansOrEvaluations() {
        RecoveryPlan original = normal(
                PLAN_1, PROVIDER_1, RecoveryPlanType.WORKSHOP_REPAIR, "80.0000"
        );

        RecoveryPlanSelectionResult result = selector.select(scored(original, emergency(PLAN_3)));

        assertEquals(RecoveryPlanStatus.FEASIBLE, original.status());
        assertEquals(null, original.selectedAt());
        assertFalse(original.evaluation().explanationCodes()
                .contains(RecoveryPlanSelector.PLAN_SELECTED_HIGHEST_UTILITY));
        assertNotSame(original, result.selectedPlan());
        assertTrue(result.selectedPlan().evaluation().explanationCodes()
                .contains(RecoveryPlanSelector.PLAN_SELECTED_HIGHEST_UTILITY));
    }

    @Test
    void rejectsUnsupportedEvaluatorVersionAndMixedDecisionSnapshots() {
        RecoveryPlan unsupported = withEvaluatorVersion(
                normal(PLAN_1, PROVIDER_1, RecoveryPlanType.WORKSHOP_REPAIR, "80.0000"),
                "future-evaluator"
        );
        assertThrows(IllegalArgumentException.class, () -> selector.select(scored(
                unsupported, emergency(PLAN_3)
        )));

        RecoveryPlan differentTrip = withTrip(
                normal(PLAN_2, PROVIDER_2, RecoveryPlanType.TOW_REPAIR_OR_TRANSFER, "70.0000"),
                id("00000000-0000-4000-8600-000000000099")
        );
        assertThrows(IllegalArgumentException.class, () -> selector.select(scored(
                normal(PLAN_1, PROVIDER_1, RecoveryPlanType.WORKSHOP_REPAIR, "80.0000"),
                differentTrip,
                emergency(PLAN_3)
        )));
    }

    @Test
    void rejectsUtilityThatDoesNotMatchStoredFormula() {
        RecoveryPlan plan = normal(
                PLAN_1, PROVIDER_1, RecoveryPlanType.WORKSHOP_REPAIR, "80.0000"
        );
        PlanEvaluation evaluation = plan.evaluation();
        RecoveryPlan malformed = copy(plan, new PlanEvaluation(
                evaluation.planId(), evaluation.feasible(), evaluation.violatedHardConstraints(),
                evaluation.warnings(), evaluation.softConstraintResults(), evaluation.riskScore(),
                evaluation.uncertaintyScore(), evaluation.benefitScore(), evaluation.lambdaCoefficient(),
                evaluation.muCoefficient(), new BigDecimal("81.0000"), evaluation.confidence(),
                evaluation.freshnessSummary(), evaluation.explanationCodes(), evaluation.evaluatedAt(),
                evaluation.evaluatorVersion(), evaluation.inputHash()
        ));

        assertThrows(IllegalArgumentException.class, () -> selector.select(scored(
                malformed, emergency(PLAN_3)
        )));
    }

    private static RecoveryPlanScoringResult scored(RecoveryPlan... plans) {
        return new RecoveryPlanScoringResult(List.of(plans));
    }

    private static RecoveryPlan normal(
            UUID planId,
            UUID providerId,
            RecoveryPlanType type,
            String utility
    ) {
        return normal(planId, providerId, type, new BigDecimal(utility));
    }

    private static RecoveryPlan normal(
            UUID planId,
            UUID providerId,
            RecoveryPlanType type,
            BigDecimal utility
    ) {
        return plan(planId, providerId, type, true, utility, List.of());
    }

    private static RecoveryPlan rejected(UUID planId, String utility, String explanationCode) {
        return plan(
                planId,
                PROVIDER_1,
                RecoveryPlanType.WORKSHOP_REPAIR,
                false,
                new BigDecimal(utility),
                List.of(explanationCode)
        );
    }

    private static RecoveryPlan emergency(UUID planId) {
        PlanEvaluation evaluation = evaluation(
                planId,
                true,
                BigDecimal.ZERO.setScale(4),
                new BigDecimal("100.0000"),
                new BigDecimal("100.0000"),
                new BigDecimal("-55.0000"),
                List.of()
        );
        return new RecoveryPlan(
                planId, TRIP_ID, DISRUPTION_ID, RecoveryPlanType.EMERGENCY_ESCALATION,
                RecoveryPlanStatus.FEASIBLE, null, null, List.of(step()), evaluation, NOW, null
        );
    }

    private static RecoveryPlan plan(
            UUID planId,
            UUID providerId,
            RecoveryPlanType type,
            boolean feasible,
            BigDecimal utility,
            List<String> explanationCodes
    ) {
        PlanEvaluation evaluation = evaluation(
                planId, feasible, utility, BigDecimal.ZERO.setScale(4),
                BigDecimal.ZERO.setScale(4), utility, explanationCodes
        );
        return new RecoveryPlan(
                planId, TRIP_ID, DISRUPTION_ID, type,
                feasible ? RecoveryPlanStatus.FEASIBLE : RecoveryPlanStatus.REJECTED,
                providerId, null, List.of(step()), evaluation, NOW, null
        );
    }

    private static PlanEvaluation evaluation(
            UUID planId,
            boolean feasible,
            BigDecimal benefit,
            BigDecimal risk,
            BigDecimal uncertainty,
            BigDecimal utility,
            List<String> explanationCodes
    ) {
        return new PlanEvaluation(
                planId,
                feasible,
                feasible ? List.of() : List.of("HARD-RULE-001"),
                List.of(),
                Map.of("TEST_SCORE", benefit),
                risk,
                uncertainty,
                benefit,
                new BigDecimal("0.3500"),
                new BigDecimal("0.2000"),
                utility,
                0.95,
                Map.of("vehicle", "FRESH"),
                explanationCodes,
                NOW,
                RecoveryPlanScorer.EVALUATOR_VERSION,
                "a".repeat(64)
        );
    }

    private static RecoveryPlanStep step() {
        return new RecoveryPlanStep(
                1, "TEST_ACTION", "Perform test action", null, NOW, NOW.plusSeconds(60), List.of()
        );
    }

    private static RecoveryPlan plan(RecoveryPlanSelectionResult result, UUID planId) {
        return result.plans().stream().filter(plan -> plan.id().equals(planId)).findFirst().orElseThrow();
    }

    private static RecoveryPlan withEvaluatorVersion(RecoveryPlan plan, String evaluatorVersion) {
        PlanEvaluation evaluation = plan.evaluation();
        return copy(plan, new PlanEvaluation(
                evaluation.planId(), evaluation.feasible(), evaluation.violatedHardConstraints(),
                evaluation.warnings(), evaluation.softConstraintResults(), evaluation.riskScore(),
                evaluation.uncertaintyScore(), evaluation.benefitScore(), evaluation.lambdaCoefficient(),
                evaluation.muCoefficient(), evaluation.finalUtility(), evaluation.confidence(),
                evaluation.freshnessSummary(), evaluation.explanationCodes(), evaluation.evaluatedAt(),
                evaluatorVersion, evaluation.inputHash()
        ));
    }

    private static RecoveryPlan withTrip(RecoveryPlan plan, UUID tripId) {
        return new RecoveryPlan(
                plan.id(), tripId, plan.disruptionId(), plan.type(), plan.status(), plan.providerId(),
                plan.safeStopId(), plan.steps(), plan.evaluation(), plan.generatedAt(), plan.selectedAt()
        );
    }

    private static RecoveryPlan copy(RecoveryPlan plan, PlanEvaluation evaluation) {
        return new RecoveryPlan(
                plan.id(), plan.tripId(), plan.disruptionId(), plan.type(), plan.status(), plan.providerId(),
                plan.safeStopId(), plan.steps(), evaluation, plan.generatedAt(), plan.selectedAt()
        );
    }

    private static UUID id(String value) {
        return UUID.fromString(value);
    }
}
