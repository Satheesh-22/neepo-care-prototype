package com.neepo.care.detection;

import com.neepo.care.context.ContextFreshnessResult;

import java.util.List;

public record DisruptionDetectionResult(
        ContextFreshnessResult freshness,
        List<DetectedDisruption> disruptions,
        List<DisruptionRuleAssessment> ruleAssessments
) {
    public DisruptionDetectionResult {
        disruptions = List.copyOf(disruptions);
        ruleAssessments = List.copyOf(ruleAssessments);
    }
}
