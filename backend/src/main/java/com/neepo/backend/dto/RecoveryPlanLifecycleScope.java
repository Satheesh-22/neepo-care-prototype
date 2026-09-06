package com.neepo.backend.dto;

import com.neepo.contracts.RecoveryPlanStatus;

import java.util.Objects;
import java.util.UUID;

public record RecoveryPlanLifecycleScope(
        UUID planId,
        UUID tripId,
        UUID disruptionId,
        RecoveryPlanStatus status
) {
    public RecoveryPlanLifecycleScope {
        Objects.requireNonNull(planId, "Plan id is required");
        Objects.requireNonNull(tripId, "Trip id is required");
        Objects.requireNonNull(disruptionId, "Disruption id is required");
        Objects.requireNonNull(status, "Plan status is required");
    }
}
