package com.neepo.care.scoring;

import com.neepo.contracts.RecoveryPlan;

import java.util.List;

public record RecoveryPlanScoringResult(List<RecoveryPlan> plans) {
    public RecoveryPlanScoringResult {
        plans = List.copyOf(plans);
    }

    public List<RecoveryPlan> feasiblePlans() {
        return plans.stream().filter(plan -> plan.evaluation().feasible()).toList();
    }

    public List<RecoveryPlan> rejectedPlans() {
        return plans.stream().filter(plan -> !plan.evaluation().feasible()).toList();
    }
}
