package com.neepo.backend.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReplanningCycleRecord(
        UUID id,
        UUID tripId,
        UUID disruptionId,
        UUID invalidationId,
        int cycleNumber,
        String status,
        Instant startedAt,
        Instant completedAt,
        UUID selectedPlanId
) {
    public ReplanningCycleRecord {
        Objects.requireNonNull(id, "Replanning cycle id is required");
        Objects.requireNonNull(tripId, "Replanning trip id is required");
        Objects.requireNonNull(disruptionId, "Replanning disruption id is required");
        Objects.requireNonNull(invalidationId, "Replanning invalidation id is required");
        Objects.requireNonNull(status, "Replanning cycle status is required");
        Objects.requireNonNull(startedAt, "Replanning cycle start time is required");
        if (cycleNumber <= 0) {
            throw new IllegalArgumentException("Replanning cycle number must be positive");
        }
        if (!status.equals("STARTED") && !status.equals("COMPLETED") && !status.equals("FAILED")) {
            throw new IllegalArgumentException("Unsupported replanning cycle status");
        }
        if (completedAt != null && completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Replanning completion cannot precede its start");
        }
        if (status.equals("STARTED") && (completedAt != null || selectedPlanId != null)) {
            throw new IllegalArgumentException("Started replanning cycle cannot have a terminal result");
        }
        if (status.equals("COMPLETED") && (completedAt == null || selectedPlanId == null)) {
            throw new IllegalArgumentException("Completed replanning cycle requires time and selected plan");
        }
        if (status.equals("FAILED") && (completedAt == null || selectedPlanId != null)) {
            throw new IllegalArgumentException("Failed replanning cycle requires only its failure time");
        }
    }
}
