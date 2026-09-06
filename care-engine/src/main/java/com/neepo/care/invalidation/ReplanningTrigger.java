package com.neepo.care.invalidation;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReplanningTrigger(
        UUID tripId,
        UUID disruptionId,
        UUID invalidationId,
        UUID sourceEventId,
        Instant requestedAt
) {
    public ReplanningTrigger {
        Objects.requireNonNull(tripId, "Replanning trip id is required");
        Objects.requireNonNull(disruptionId, "Replanning disruption id is required");
        Objects.requireNonNull(invalidationId, "Replanning invalidation id is required");
        Objects.requireNonNull(sourceEventId, "Replanning source event id is required");
        Objects.requireNonNull(requestedAt, "Replanning request time is required");
    }
}
