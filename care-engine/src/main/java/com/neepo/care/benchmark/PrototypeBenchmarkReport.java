package com.neepo.care.benchmark;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record PrototypeBenchmarkReport(
        BenchmarkMetadata metadata,
        Map<String, String> configuration,
        List<StrategyMetrics> strategies
) {
    public PrototypeBenchmarkReport {
        metadata = Objects.requireNonNull(metadata, "Benchmark metadata is required");
        configuration = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(configuration, "Benchmark configuration is required")
        ));
        strategies = List.copyOf(Objects.requireNonNull(strategies, "Benchmark strategies are required"));
        if (strategies.isEmpty()) {
            throw new IllegalArgumentException("Benchmark report requires strategy metrics");
        }
    }
}
