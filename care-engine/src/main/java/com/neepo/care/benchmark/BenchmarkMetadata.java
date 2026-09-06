package com.neepo.care.benchmark;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record BenchmarkMetadata(
        String scenarioCode,
        String datasetVersion,
        String environment,
        String commitHash,
        Instant measuredAt,
        List<String> limitations
) {
    public BenchmarkMetadata {
        requireText(scenarioCode, "Benchmark scenario code is required");
        requireText(datasetVersion, "Benchmark dataset version is required");
        requireText(environment, "Benchmark environment is required");
        requireText(commitHash, "Benchmark commit hash is required");
        Objects.requireNonNull(measuredAt, "Benchmark measurement time is required");
        limitations = List.copyOf(Objects.requireNonNull(limitations, "Benchmark limitations are required"));
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
