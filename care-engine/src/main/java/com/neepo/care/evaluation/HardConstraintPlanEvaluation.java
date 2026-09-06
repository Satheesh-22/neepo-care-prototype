package com.neepo.care.evaluation;

import com.neepo.contracts.RecoveryPlan;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Internal hard-gate output. Soft evaluation later composes this into the frozen PlanEvaluation contract.
 */
public record HardConstraintPlanEvaluation(
        RecoveryPlan plan,
        List<HardConstraintAssessment> assessments,
        List<String> warnings,
        double confidence,
        Map<String, String> freshnessSummary,
        Instant evaluatedAt,
        String evaluatorVersion,
        String inputHash
) {
    public HardConstraintPlanEvaluation {
        Objects.requireNonNull(plan, "Recovery plan is required");
        assessments = List.copyOf(assessments);
        warnings = warnings == null ? List.of() : warnings.stream().distinct().toList();
        freshnessSummary = Collections.unmodifiableMap(new LinkedHashMap<>(freshnessSummary));
        Objects.requireNonNull(evaluatedAt, "Evaluation time is required");
        if (evaluatorVersion == null || evaluatorVersion.isBlank()) {
            throw new IllegalArgumentException("Evaluator version is required");
        }
        if (inputHash == null || !inputHash.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Evaluation input hash must be lowercase SHA-256 hex");
        }
        if (!Double.isFinite(confidence) || confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("Evaluation confidence must be between 0 and 1");
        }
    }

    public boolean feasible() {
        return assessments.stream().noneMatch(assessment ->
                assessment.status() == HardConstraintStatus.VIOLATED);
    }

    public List<String> violatedHardConstraints() {
        return assessments.stream()
                .filter(assessment -> assessment.status() == HardConstraintStatus.VIOLATED)
                .map(HardConstraintAssessment::ruleId)
                .toList();
    }

    public List<String> explanationCodes() {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        assessments.forEach(assessment -> codes.addAll(assessment.explanationCodes()));
        return List.copyOf(codes);
    }
}
