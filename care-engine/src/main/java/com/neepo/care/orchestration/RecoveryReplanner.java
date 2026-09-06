package com.neepo.care.orchestration;

import com.neepo.care.detection.DetectedDisruption;
import com.neepo.care.invalidation.ReplanningTrigger;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.TripContext;

import java.util.Objects;

public final class RecoveryReplanner {
    private final RecoveryDecisionEngine decisionEngine;

    public RecoveryReplanner(RecoveryDecisionEngine decisionEngine) {
        this.decisionEngine = Objects.requireNonNull(decisionEngine, "Recovery decision engine is required");
    }

    public RecoveryReplanningResult replan(
            TripContext context,
            ReplanningTrigger trigger,
            DetectedDisruption activeDisruption
    ) {
        Objects.requireNonNull(context, "Replanning trip context is required");
        Objects.requireNonNull(context.trip(), "Replanning context trip is required");
        Objects.requireNonNull(context.evaluationTime(), "Replanning context evaluation time is required");
        Objects.requireNonNull(trigger, "Replanning trigger is required");
        Objects.requireNonNull(activeDisruption, "Active disruption is required for replanning");
        RecoveryPlan invalidatedPlan = Objects.requireNonNull(
                context.currentRecoveryPlan(),
                "Replanning context must contain the invalidated recovery plan"
        );

        if (invalidatedPlan.status() != RecoveryPlanStatus.INVALIDATED) {
            throw new IllegalArgumentException("Replanning requires an INVALIDATED current recovery plan");
        }
        if (!trigger.tripId().equals(context.trip().id())
                || !trigger.tripId().equals(invalidatedPlan.tripId())) {
            throw new IllegalArgumentException("Replanning trigger trip does not match the context and plan");
        }
        if (!trigger.disruptionId().equals(invalidatedPlan.disruptionId())) {
            throw new IllegalArgumentException("Replanning trigger disruption does not match the invalidated plan");
        }
        if (trigger.requestedAt().isAfter(context.evaluationTime())) {
            throw new IllegalArgumentException("Replanning trigger cannot follow context evaluation time");
        }
        if (activeDisruption.detectedAt() == null
                || activeDisruption.detectedAt().isAfter(context.evaluationTime())) {
            throw new IllegalArgumentException("Active disruption must be confirmed by the replanning snapshot");
        }

        RecoveryDecisionResult decision = decisionEngine.redecide(
                context,
                trigger.disruptionId(),
                activeDisruption
        );
        if (decision.selection().selectedPlan().id().equals(invalidatedPlan.id())) {
            throw new IllegalStateException("Replanning cannot reselect the invalidated plan instance");
        }
        return new RecoveryReplanningResult(trigger, decision);
    }
}
