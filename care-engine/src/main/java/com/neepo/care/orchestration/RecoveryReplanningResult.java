package com.neepo.care.orchestration;

import com.neepo.care.invalidation.ReplanningTrigger;

import java.util.Objects;

public record RecoveryReplanningResult(
        ReplanningTrigger trigger,
        RecoveryDecisionResult decision
) {
    public RecoveryReplanningResult {
        trigger = Objects.requireNonNull(trigger, "Replanning trigger is required");
        decision = Objects.requireNonNull(decision, "Recovery decision is required");
        if (!trigger.tripId().equals(decision.selection().selectedPlan().tripId())
                || !trigger.disruptionId().equals(decision.selection().selectedPlan().disruptionId())) {
            throw new IllegalArgumentException("Replanning decision must match the trigger trip and disruption");
        }
    }
}
