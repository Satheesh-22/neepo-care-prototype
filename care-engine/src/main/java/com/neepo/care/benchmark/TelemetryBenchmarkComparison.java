package com.neepo.care.benchmark;

public record TelemetryBenchmarkComparison(
        int adaptiveTransmissions,
        long adaptiveBandwidthBytes,
        int fixedTransmissions,
        long fixedBandwidthBytes,
        int fixedIntervalSeconds,
        int payloadBytes
) {
    public TelemetryBenchmarkComparison {
        if (adaptiveTransmissions < 0
                || adaptiveBandwidthBytes < 0
                || fixedTransmissions < 0
                || fixedBandwidthBytes < 0
                || fixedIntervalSeconds <= 0
                || payloadBytes <= 0) {
            throw new IllegalArgumentException("Telemetry benchmark values must be nonnegative and configured");
        }
    }
}
