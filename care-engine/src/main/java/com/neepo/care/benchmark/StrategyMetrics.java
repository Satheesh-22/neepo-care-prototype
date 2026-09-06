package com.neepo.care.benchmark;

public record StrategyMetrics(
        String strategyCode,
        boolean successfulRecovery,
        int invalidRecommendations,
        int recoveryTimeSeconds,
        int reroutingOperations,
        int telemetryTransmissions,
        long estimatedBandwidthBytes,
        int decisionLatencyMs,
        int staleDataDecisions
) {
    public StrategyMetrics {
        if (strategyCode == null || strategyCode.isBlank()) {
            throw new IllegalArgumentException("Benchmark strategy code is required");
        }
        if (invalidRecommendations < 0
                || recoveryTimeSeconds < 0
                || reroutingOperations < 0
                || telemetryTransmissions < 0
                || estimatedBandwidthBytes < 0
                || decisionLatencyMs < 0
                || staleDataDecisions < 0) {
            throw new IllegalArgumentException("Benchmark metrics must be nonnegative");
        }
    }
}
