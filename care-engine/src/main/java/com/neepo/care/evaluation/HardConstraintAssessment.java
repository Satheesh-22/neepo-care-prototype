package com.neepo.care.evaluation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record HardConstraintAssessment(
        String ruleId,
        HardConstraintStatus status,
        List<String> explanationCodes,
        Map<String, Object> evidence
) {
    public HardConstraintAssessment {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("Rule id is required");
        }
        Objects.requireNonNull(status, "Constraint status is required");
        explanationCodes = explanationCodes == null
                ? List.of()
                : explanationCodes.stream().distinct().toList();
        if (evidence == null) {
            evidence = Map.of();
        } else {
            TreeMap<String, Object> immutableEvidence = new TreeMap<>();
            evidence.forEach((key, value) -> immutableEvidence.put(key, immutableValue(value)));
            evidence = Collections.unmodifiableMap(immutableEvidence);
        }
        if (status == HardConstraintStatus.VIOLATED && explanationCodes.isEmpty()) {
            throw new IllegalArgumentException("A hard violation requires an explanation code");
        }
        if (status != HardConstraintStatus.VIOLATED && !explanationCodes.isEmpty()) {
            throw new IllegalArgumentException("Only hard violations may contain explanation codes");
        }
    }

    private static Object immutableValue(Object value) {
        if (value instanceof List<?> values) {
            return values.stream().map(HardConstraintAssessment::immutableValue).toList();
        }
        return value;
    }
}
