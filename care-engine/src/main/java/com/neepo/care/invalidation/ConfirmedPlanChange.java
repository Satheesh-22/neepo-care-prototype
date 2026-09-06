package com.neepo.care.invalidation;

import com.neepo.contracts.PlanInvalidationReason;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ConfirmedPlanChange(
        UUID eventId,
        PlanInvalidationReason reason,
        Instant observedAt,
        String details
) {
    public ConfirmedPlanChange {
        Objects.requireNonNull(eventId, "Invalidation event id is required");
        Objects.requireNonNull(reason, "Invalidation reason is required");
        Objects.requireNonNull(observedAt, "Invalidation observation time is required");
        if (details != null && details.length() > 500) {
            throw new IllegalArgumentException("Invalidation details cannot exceed 500 characters");
        }
    }
}
