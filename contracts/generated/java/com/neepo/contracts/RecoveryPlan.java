package com.neepo.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecoveryPlan(
        UUID id,
        UUID tripId,
        UUID disruptionId,
        RecoveryPlanType type,
        RecoveryPlanStatus status,
        UUID providerId,
        UUID safeStopId,
        List<RecoveryPlanStep> steps,
        PlanEvaluation evaluation,
        Instant generatedAt,
        Instant selectedAt
) {
}
