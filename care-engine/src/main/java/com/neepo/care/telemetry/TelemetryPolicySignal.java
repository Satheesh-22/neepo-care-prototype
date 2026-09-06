package com.neepo.care.telemetry;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TelemetryPolicySignal(
        UUID eventId,
        Instant observedAt,
        boolean nearDecisionPoint,
        boolean safelyParked
) {
    public TelemetryPolicySignal {
        Objects.requireNonNull(eventId, "Telemetry policy event id is required");
        Objects.requireNonNull(observedAt, "Telemetry policy observation time is required");
    }
}
