package com.neepo.care.evaluation;

import java.util.List;

public record HardConstraintEvaluationResult(List<HardConstraintPlanEvaluation> planEvaluations) {
    public HardConstraintEvaluationResult {
        planEvaluations = List.copyOf(planEvaluations);
    }

    public List<HardConstraintPlanEvaluation> feasiblePlans() {
        return planEvaluations.stream().filter(HardConstraintPlanEvaluation::feasible).toList();
    }

    public List<HardConstraintPlanEvaluation> rejectedPlans() {
        return planEvaluations.stream().filter(evaluation -> !evaluation.feasible()).toList();
    }
}
