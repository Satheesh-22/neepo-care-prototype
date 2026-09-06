package com.neepo.care.detection;

import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record DetectedDisruption(
        DisruptionType type,
        DisruptionSeverity severity,
        Instant detectedAt,
        List<String> explanationCodes,
        Map<String, Object> evidence
) {
    public DetectedDisruption {
        explanationCodes = List.copyOf(explanationCodes);
        evidence = Collections.unmodifiableMap(new TreeMap<>(evidence));
    }
}
