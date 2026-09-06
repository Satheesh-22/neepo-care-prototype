package com.neepo.care.benchmark;

import com.neepo.care.telemetry.TelemetryPolicyDecision;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record TelemetryPolicyWindow(
        TelemetryPolicyDecision decision,
        Instant endsAt
) {
    public TelemetryPolicyWindow {
        decision = Objects.requireNonNull(decision, "Telemetry policy decision is required");
        endsAt = Objects.requireNonNull(endsAt, "Telemetry policy window end is required");
        if (Duration.between(decision.decidedAt(), endsAt).toSeconds() < 1) {
            throw new IllegalArgumentException("Telemetry policy window must be at least one second");
        }
    }
}
