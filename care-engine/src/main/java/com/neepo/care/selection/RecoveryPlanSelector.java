package com.neepo.care.selection;

import com.neepo.care.scoring.RecoveryPlanScorer;
import com.neepo.care.scoring.RecoveryPlanScoringResult;
import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public final class RecoveryPlanSelector {
    public static final String PLAN_SELECTED_HIGHEST_UTILITY = "PLAN_SELECTED_HIGHEST_UTILITY";
    public static final String PLAN_SELECTED_TIE_BREAK = "PLAN_SELECTED_TIE_BREAK";
    public static final String NO_FEASIBLE_RECOVERY_PLAN = "NO_FEASIBLE_RECOVERY_PLAN";
    public static final String EMERGENCY_ESCALATION_SELECTED = "EMERGENCY_ESCALATION_SELECTED";
    public static final String DETERMINISTIC_TIE_BREAK_RULE = "FINAL_UTILITY_DESC_THEN_PLAN_UUID_ASC";

    private static final Comparator<RecoveryPlan> CANONICAL_PLAN_ORDER = Comparator
            .comparingInt((RecoveryPlan plan) -> plan.type().ordinal())
            .thenComparing(plan -> nullableId(plan.providerId()))
            .thenComparing(plan -> nullableId(plan.safeStopId()))
            .thenComparing(plan -> plan.id().toString());
    private static final Comparator<RecoveryPlan> SELECTION_ORDER = (left, right) -> {
        int utilityOrder = right.evaluation().finalUtility().compareTo(left.evaluation().finalUtility());
        return utilityOrder != 0 ? utilityOrder : left.id().toString().compareTo(right.id().toString());
    };

    public RecoveryPlanSelectionResult select(RecoveryPlanScoringResult scoringResult) {
        Objects.requireNonNull(scoringResult, "Scoring result is required");
        List<RecoveryPlan> scoredPlans = scoringResult.plans();
        SelectionContext context = validate(scoredPlans);

        List<RecoveryPlan> normalFeasible = scoredPlans.stream()
                .filter(plan -> plan.evaluation().feasible())
                .filter(plan -> plan.type() != RecoveryPlanType.EMERGENCY_ESCALATION)
                .sorted(SELECTION_ORDER)
                .toList();

        RecoveryPlan selected;
        List<String> selectionCodes;
        if (!normalFeasible.isEmpty()) {
            selected = normalFeasible.getFirst();
            boolean tied = normalFeasible.size() > 1
                    && selected.evaluation().finalUtility()
                    .compareTo(normalFeasible.get(1).evaluation().finalUtility()) == 0;
            selectionCodes = tied
                    ? List.of(PLAN_SELECTED_HIGHEST_UTILITY, PLAN_SELECTED_TIE_BREAK)
                    : List.of(PLAN_SELECTED_HIGHEST_UTILITY);
        } else {
            List<RecoveryPlan> emergencyCandidates = scoredPlans.stream()
                    .filter(plan -> plan.evaluation().feasible())
                    .filter(plan -> plan.type() == RecoveryPlanType.EMERGENCY_ESCALATION)
                    .toList();
            if (emergencyCandidates.size() != 1) {
                throw new IllegalArgumentException(
                        "Exactly one feasible emergency escalation is required when no normal plan is feasible"
                );
            }
            selected = emergencyCandidates.getFirst();
            selectionCodes = List.of(NO_FEASIBLE_RECOVERY_PLAN, EMERGENCY_ESCALATION_SELECTED);
        }

        RecoveryPlan selectedPlan = selectedPlan(selected, selectionCodes, context.evaluatedAt());
        List<RecoveryPlan> resultingPlans = scoredPlans.stream()
                .map(plan -> plan.id().equals(selectedPlan.id()) ? selectedPlan : plan)
                .sorted(CANONICAL_PLAN_ORDER)
                .toList();
        DecisionExplanation explanation = explanation(
                context,
                selectedPlan,
                scoredPlans,
                selectionCodes
        );
        return new RecoveryPlanSelectionResult(resultingPlans, selectedPlan, explanation);
    }

    private static SelectionContext validate(List<RecoveryPlan> plans) {
        Objects.requireNonNull(plans, "Scored plans are required");
        if (plans.isEmpty()) {
            throw new IllegalArgumentException("At least one scored plan is required for selection");
        }

        UUID tripId = null;
        UUID disruptionId = null;
        Instant evaluatedAt = null;
        Set<UUID> planIds = new HashSet<>();
        for (RecoveryPlan plan : plans) {
            Objects.requireNonNull(plan, "Scored plan entry is required");
            Objects.requireNonNull(plan.id(), "Scored plan id is required");
            Objects.requireNonNull(plan.tripId(), "Scored plan trip id is required");
            Objects.requireNonNull(plan.disruptionId(), "Scored plan disruption id is required");
            Objects.requireNonNull(plan.type(), "Scored plan type is required");
            Objects.requireNonNull(plan.status(), "Scored plan status is required");
            PlanEvaluation evaluation = Objects.requireNonNull(
                    plan.evaluation(),
                    "Scored plan evaluation is required"
            );
            if (!planIds.add(plan.id())) {
                throw new IllegalArgumentException("Duplicate scored plan id: " + plan.id());
            }
            if (!plan.id().equals(evaluation.planId())) {
                throw new IllegalArgumentException("Plan evaluation id does not match the plan id");
            }
            RecoveryPlanStatus expectedStatus = evaluation.feasible()
                    ? RecoveryPlanStatus.FEASIBLE
                    : RecoveryPlanStatus.REJECTED;
            if (plan.status() != expectedStatus || plan.selectedAt() != null) {
                throw new IllegalArgumentException("Selection requires unselected FEASIBLE or REJECTED scored plans");
            }
            if (!RecoveryPlanScorer.EVALUATOR_VERSION.equals(evaluation.evaluatorVersion())) {
                throw new IllegalArgumentException("Unsupported plan evaluator version");
            }
            validateEvaluation(evaluation);

            tripId = sameValue(tripId, plan.tripId(), "Scored plans must share one trip id");
            disruptionId = sameValue(
                    disruptionId,
                    plan.disruptionId(),
                    "Scored plans must share one disruption id"
            );
            evaluatedAt = sameValue(
                    evaluatedAt,
                    evaluation.evaluatedAt(),
                    "Scored plans must share one evaluation time"
            );
        }
        return new SelectionContext(tripId, disruptionId, evaluatedAt);
    }

    private static void validateEvaluation(PlanEvaluation evaluation) {
        Objects.requireNonNull(evaluation.evaluatedAt(), "Evaluation time is required");
        Objects.requireNonNull(evaluation.violatedHardConstraints(), "Hard-constraint violations are required");
        Objects.requireNonNull(evaluation.warnings(), "Evaluation warnings are required");
        Objects.requireNonNull(evaluation.softConstraintResults(), "Soft-constraint results are required");
        BigDecimal risk = Objects.requireNonNull(evaluation.riskScore(), "Risk score is required");
        BigDecimal uncertainty = Objects.requireNonNull(
                evaluation.uncertaintyScore(),
                "Uncertainty score is required"
        );
        BigDecimal benefit = Objects.requireNonNull(evaluation.benefitScore(), "Benefit score is required");
        BigDecimal lambda = Objects.requireNonNull(evaluation.lambdaCoefficient(), "Lambda is required");
        BigDecimal mu = Objects.requireNonNull(evaluation.muCoefficient(), "Mu is required");
        BigDecimal utility = Objects.requireNonNull(evaluation.finalUtility(), "Final utility is required");
        Objects.requireNonNull(evaluation.freshnessSummary(), "Freshness summary is required");
        Objects.requireNonNull(evaluation.explanationCodes(), "Evaluation explanation codes are required");
        Objects.requireNonNull(evaluation.inputHash(), "Evaluation input hash is required");
        if (Double.isNaN(evaluation.confidence()) || Double.isInfinite(evaluation.confidence())) {
            throw new IllegalArgumentException("Evaluation confidence must be finite");
        }
        BigDecimal expectedUtility = benefit
                .subtract(lambda.multiply(risk))
                .subtract(mu.multiply(uncertainty))
                .setScale(4, RoundingMode.HALF_UP);
        if (expectedUtility.compareTo(utility) != 0) {
            throw new IllegalArgumentException("Final utility does not match the scored evaluation formula");
        }
    }

    private static RecoveryPlan selectedPlan(
            RecoveryPlan plan,
            List<String> selectionCodes,
            Instant selectedAt
    ) {
        PlanEvaluation evaluation = plan.evaluation();
        LinkedHashSet<String> explanationCodes = new LinkedHashSet<>(evaluation.explanationCodes());
        explanationCodes.addAll(selectionCodes);
        PlanEvaluation selectedEvaluation = new PlanEvaluation(
                evaluation.planId(),
                evaluation.feasible(),
                List.copyOf(evaluation.violatedHardConstraints()),
                List.copyOf(evaluation.warnings()),
                immutableMap(evaluation.softConstraintResults()),
                evaluation.riskScore(),
                evaluation.uncertaintyScore(),
                evaluation.benefitScore(),
                evaluation.lambdaCoefficient(),
                evaluation.muCoefficient(),
                evaluation.finalUtility(),
                evaluation.confidence(),
                immutableMap(evaluation.freshnessSummary()),
                List.copyOf(explanationCodes),
                evaluation.evaluatedAt(),
                evaluation.evaluatorVersion(),
                evaluation.inputHash()
        );
        return new RecoveryPlan(
                plan.id(),
                plan.tripId(),
                plan.disruptionId(),
                plan.type(),
                RecoveryPlanStatus.SELECTED,
                plan.providerId(),
                plan.safeStopId(),
                List.copyOf(plan.steps()),
                selectedEvaluation,
                plan.generatedAt(),
                selectedAt
        );
    }

    private static DecisionExplanation explanation(
            SelectionContext context,
            RecoveryPlan selectedPlan,
            List<RecoveryPlan> scoredPlans,
            List<String> selectionCodes
    ) {
        List<RecoveryPlan> rejectedPlans = scoredPlans.stream()
                .filter(plan -> plan.status() == RecoveryPlanStatus.REJECTED)
                .sorted(Comparator.comparing(plan -> plan.id().toString()))
                .toList();
        List<UUID> rejectedPlanIds = rejectedPlans.stream().map(RecoveryPlan::id).toList();
        TreeMap<UUID, List<String>> rejectionCodes = new TreeMap<>(Comparator.comparing(UUID::toString));
        for (RecoveryPlan rejected : rejectedPlans) {
            rejectionCodes.put(rejected.id(), List.copyOf(rejected.evaluation().explanationCodes()));
        }
        return new DecisionExplanation(
                context.tripId(),
                context.disruptionId(),
                selectedPlan.id(),
                rejectedPlanIds,
                List.copyOf(selectionCodes),
                Collections.unmodifiableMap(new LinkedHashMap<>(rejectionCodes)),
                DETERMINISTIC_TIE_BREAK_RULE,
                context.evaluatedAt()
        );
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private static <T> T sameValue(T current, T candidate, String message) {
        Objects.requireNonNull(candidate, message);
        if (current != null && !current.equals(candidate)) {
            throw new IllegalArgumentException(message);
        }
        return candidate;
    }

    private static String nullableId(UUID value) {
        return value == null ? "-" : value.toString();
    }

    private record SelectionContext(UUID tripId, UUID disruptionId, Instant evaluatedAt) {
    }
}
