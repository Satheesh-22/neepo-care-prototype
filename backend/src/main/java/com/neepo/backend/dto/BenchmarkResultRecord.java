package com.neepo.backend.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BenchmarkResultRecord(
        UUID id,
        String scenarioCode,
        String strategyCode,
        String datasetVersion,
        boolean successfulRecovery,
        int invalidRecommendations,
        int recoveryTimeSeconds,
        int reroutingOperations,
        int telemetryTransmissions,
        long estimatedBandwidthBytes,
        int decisionLatencyMs,
        int staleDataDecisions,
        Instant measuredAt,
        String configurationJson
) {
    public BenchmarkResultRecord {
        Objects.requireNonNull(id, "Benchmark result id is required");
        requireText(scenarioCode, "Benchmark scenario code is required");
        requireText(strategyCode, "Benchmark strategy code is required");
        requireText(datasetVersion, "Benchmark dataset version is required");
        Objects.requireNonNull(measuredAt, "Benchmark measurement time is required");
        requireText(configurationJson, "Benchmark configuration is required");
        if (invalidRecommendations < 0 || recoveryTimeSeconds < 0 || reroutingOperations < 0
                || telemetryTransmissions < 0 || estimatedBandwidthBytes < 0
                || decisionLatencyMs < 0 || staleDataDecisions < 0) {
            throw new IllegalArgumentException("Benchmark metrics must be nonnegative");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
