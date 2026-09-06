package com.neepo.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PlanEvaluation(
        UUID planId,
        boolean feasible,
        List<String> violatedHardConstraints,
        List<String> warnings,
        Map<String, BigDecimal> softConstraintResults,
        BigDecimal riskScore,
        BigDecimal uncertaintyScore,
        BigDecimal benefitScore,
        BigDecimal lambdaCoefficient,
        BigDecimal muCoefficient,
        BigDecimal finalUtility,
        double confidence,
        Map<String, String> freshnessSummary,
        List<String> explanationCodes,
        Instant evaluatedAt,
        String evaluatorVersion,
        String inputHash
) {
}
