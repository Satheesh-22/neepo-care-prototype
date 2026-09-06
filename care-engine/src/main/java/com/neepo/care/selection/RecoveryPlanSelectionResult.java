package com.neepo.care.selection;

import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.RecoveryPlan;

import java.util.List;
import java.util.Objects;

public record RecoveryPlanSelectionResult(
        List<RecoveryPlan> plans,
        RecoveryPlan selectedPlan,
        DecisionExplanation explanation
) {
    public RecoveryPlanSelectionResult {
        plans = List.copyOf(Objects.requireNonNull(plans, "Selected plan collection is required"));
        selectedPlan = Objects.requireNonNull(selectedPlan, "Selected plan is required");
        explanation = Objects.requireNonNull(explanation, "Decision explanation is required");
        var selectedPlanId = selectedPlan.id();
        if (plans.stream().noneMatch(plan -> plan.id().equals(selectedPlanId))) {
            throw new IllegalArgumentException("Selected plan must be present in the returned plan collection");
        }
    }

    public boolean emergencyFallbackSelected() {
        return selectedPlan.type() == com.neepo.contracts.RecoveryPlanType.EMERGENCY_ESCALATION;
    }
}
