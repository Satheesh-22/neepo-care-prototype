package com.neepo.care.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ContextFreshnessResult(
        boolean hardFactsUsable,
        boolean hasExpiredRequiredData,
        boolean hasUnverifiedRequiredData,
        double minimumConfidence,
        List<ObservationFreshnessAssessment> assessments,
        Map<String, String> freshnessSummary,
        List<String> explanationCodes
) {
    public ContextFreshnessResult {
        assessments = List.copyOf(assessments);
        freshnessSummary = Collections.unmodifiableMap(new LinkedHashMap<>(freshnessSummary));
        explanationCodes = List.copyOf(explanationCodes);
    }
}
