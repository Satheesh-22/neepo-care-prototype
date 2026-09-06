package com.neepo.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record TelemetryPolicyRequest(
        @NotNull UUID eventId,
        @NotNull Instant observedAt,
        boolean nearDecisionPoint,
        boolean safelyParked
) {
}
