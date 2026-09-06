package com.neepo.care.benchmark;

import com.neepo.contracts.TelemetryPolicyState;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TelemetryBenchmarkRunner {
    public TelemetryBenchmarkComparison compare(
            List<TelemetryPolicyWindow> windows,
            int fixedIntervalSeconds,
            int payloadBytes
    ) {
        if (fixedIntervalSeconds <= 0 || payloadBytes <= 0) {
            throw new IllegalArgumentException("Fixed interval and telemetry payload bytes must be positive");
        }
        List<TelemetryPolicyWindow> ordered = Objects.requireNonNull(
                windows,
                "Telemetry policy windows are required"
        ).stream().map(window -> Objects.requireNonNull(window, "Telemetry policy window is required"))
                .sorted(Comparator
                        .comparing((TelemetryPolicyWindow window) -> window.decision().decidedAt())
                        .thenComparing(window -> window.decision().eventId(), Comparator.comparing(UUID::toString)))
                .toList();
        validateWindows(ordered);

        long adaptive = 0;
        long fixed = 0;
        for (TelemetryPolicyWindow window : ordered) {
            if (window.decision().policyState() == TelemetryPolicyState.OFFLINE_BUFFERING) {
                continue;
            }
            long seconds = Duration.between(window.decision().decidedAt(), window.endsAt()).toSeconds();
            adaptive = Math.addExact(adaptive, transmissions(seconds, window.decision().intervalSeconds()));
            fixed = Math.addExact(fixed, transmissions(seconds, fixedIntervalSeconds));
        }
        int adaptiveCount = Math.toIntExact(adaptive);
        int fixedCount = Math.toIntExact(fixed);
        return new TelemetryBenchmarkComparison(
                adaptiveCount,
                Math.multiplyExact(adaptive, payloadBytes),
                fixedCount,
                Math.multiplyExact(fixed, payloadBytes),
                fixedIntervalSeconds,
                payloadBytes
        );
    }

    private static void validateWindows(List<TelemetryPolicyWindow> windows) {
        for (int index = 1; index < windows.size(); index++) {
            TelemetryPolicyWindow previous = windows.get(index - 1);
            TelemetryPolicyWindow current = windows.get(index);
            if (current.decision().decidedAt().isBefore(previous.endsAt())) {
                throw new IllegalArgumentException("Telemetry policy windows cannot overlap");
            }
            if (!current.decision().tripId().equals(previous.decision().tripId())) {
                throw new IllegalArgumentException("Telemetry benchmark windows must belong to one trip");
            }
        }
    }

    private static long transmissions(long durationSeconds, int intervalSeconds) {
        return Math.floorDiv(Math.addExact(durationSeconds, intervalSeconds - 1L), intervalSeconds);
    }
}
