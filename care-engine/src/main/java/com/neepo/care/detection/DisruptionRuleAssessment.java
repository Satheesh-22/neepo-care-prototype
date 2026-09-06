package com.neepo.care.detection;

import com.neepo.contracts.DisruptionType;

import java.util.List;

public record DisruptionRuleAssessment(
        DisruptionType type,
        DetectionRuleStatus status,
        List<String> blockingReasons
) {
    public DisruptionRuleAssessment {
        blockingReasons = blockingReasons.stream().sorted().toList();
    }
}
