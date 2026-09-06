package com.neepo.care.context;

import com.neepo.contracts.ObservationStatus;

import java.util.List;

public record ObservationFreshnessAssessment(
        String key,
        ObservationRequirement requirement,
        ObservationStatus status,
        boolean usableForDecision,
        double confidence,
        List<FreshnessIssue> issues
) {
    public ObservationFreshnessAssessment {
        issues = List.copyOf(issues);
    }
}
