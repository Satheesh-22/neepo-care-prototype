package com.neepo.care.telemetry;

import com.neepo.contracts.TelemetryPolicyState;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TelemetryPolicyDecision(
        UUID tripId,
        UUID eventId,
        TelemetryPolicyState policyState,
        Integer intervalSeconds,
        Instant decidedAt,
        String explanationCode
) {
    public TelemetryPolicyDecision {
        Objects.requireNonNull(tripId, "Telemetry policy trip id is required");
        Objects.requireNonNull(eventId, "Telemetry policy event id is required");
        Objects.requireNonNull(policyState, "Telemetry policy state is required");
        Objects.requireNonNull(decidedAt, "Telemetry policy decision time is required");
        if (explanationCode == null || explanationCode.isBlank()) {
            throw new IllegalArgumentException("Telemetry policy explanation code is required");
        }
        if (policyState == TelemetryPolicyState.OFFLINE_BUFFERING) {
            if (intervalSeconds != null) {
                throw new IllegalArgumentException("Offline buffering cannot schedule an upload interval");
            }
        } else if (intervalSeconds == null || intervalSeconds <= 0) {
            throw new IllegalArgumentException("Online telemetry policy requires a positive interval");
        }
    }
}
