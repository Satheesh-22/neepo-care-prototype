package com.neepo.care.invalidation;

import com.neepo.contracts.PlanInvalidation;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;

import java.util.Objects;

public record RecoveryPlanInvalidationResult(
        RecoveryPlan invalidatedPlan,
        PlanInvalidation invalidation,
        ReplanningTrigger replanningTrigger
) {
    public RecoveryPlanInvalidationResult {
        invalidatedPlan = Objects.requireNonNull(invalidatedPlan, "Invalidated plan is required");
        invalidation = Objects.requireNonNull(invalidation, "Plan invalidation is required");
        replanningTrigger = Objects.requireNonNull(replanningTrigger, "Replanning trigger is required");
        if (invalidatedPlan.status() != RecoveryPlanStatus.INVALIDATED) {
            throw new IllegalArgumentException("Invalidation result plan must have INVALIDATED status");
        }
        if (!invalidatedPlan.id().equals(invalidation.planId())) {
            throw new IllegalArgumentException("Invalidation must reference the invalidated plan");
        }
        if (!invalidation.id().equals(replanningTrigger.invalidationId())
                || !invalidation.eventId().equals(replanningTrigger.sourceEventId())) {
            throw new IllegalArgumentException("Replanning trigger must reference the same invalidation event");
        }
    }
}
