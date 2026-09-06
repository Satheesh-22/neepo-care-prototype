package com.neepo.contracts;

import java.time.Instant;
import java.util.UUID;

public record PlanInvalidation(
        UUID id,
        UUID eventId,
        UUID planId,
        PlanInvalidationReason reason,
        String explanationCode,
        Instant observedAt,
        String details
) {
}
