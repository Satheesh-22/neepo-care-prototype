package com.neepo.care.context;

import com.neepo.contracts.ObservationMetadata;
import com.neepo.contracts.ObservationStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public final class ContextFreshnessValidator {
    public static final String EXPIRED_CONTEXT_CODE = "CONTEXT_OBSERVATION_EXPIRED";
    public static final String LOW_CONFIDENCE_CODE = "LOW_CONTEXT_CONFIDENCE";

    private final ContextFreshnessPolicy policy;

    public ContextFreshnessValidator(ContextFreshnessPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "Freshness policy is required");
    }

    public ContextFreshnessResult validate(DecisionState state) {
        List<ObservationFreshnessAssessment> assessments = state.observations().stream()
                .map(observation -> assess(observation, state.evaluationTime()))
                .toList();

        boolean expiredRequired = assessments.stream().anyMatch(assessment ->
                assessment.requirement() == ObservationRequirement.HARD_FACT
                        && assessment.status() == ObservationStatus.EXPIRED);
        boolean unverifiedRequired = assessments.stream().anyMatch(assessment ->
                assessment.requirement() == ObservationRequirement.HARD_FACT
                        && assessment.status() == ObservationStatus.UNVERIFIED);
        boolean hardFactsUsable = assessments.stream()
                .filter(assessment -> assessment.requirement() == ObservationRequirement.HARD_FACT)
                .allMatch(ObservationFreshnessAssessment::usableForDecision);
        boolean lowConfidence = assessments.stream().anyMatch(assessment ->
                assessment.issues().contains(FreshnessIssue.BELOW_MINIMUM_CONFIDENCE));

        double minimumConfidence = state.observations().stream()
                .map(DecisionObservation::metadata)
                .filter(metadata -> metadata != null && validConfidence(metadata.confidence()))
                .mapToDouble(ObservationMetadata::confidence)
                .min()
                .orElse(0);

        TreeMap<String, String> summary = new TreeMap<>();
        assessments.forEach(assessment -> summary.put(assessment.key(), assessment.status().name()));

        List<String> explanationCodes = new ArrayList<>();
        if (expiredRequired) {
            explanationCodes.add(EXPIRED_CONTEXT_CODE);
        }
        if (lowConfidence) {
            explanationCodes.add(LOW_CONFIDENCE_CODE);
        }

        return new ContextFreshnessResult(
                hardFactsUsable,
                expiredRequired,
                unverifiedRequired,
                minimumConfidence,
                assessments,
                summary,
                explanationCodes
        );
    }

    private ObservationFreshnessAssessment assess(DecisionObservation observation, Instant evaluationTime) {
        ObservationMetadata metadata = observation.metadata();
        if (metadata == null) {
            return unverified(observation, 0, List.of(FreshnessIssue.MISSING_METADATA));
        }

        List<FreshnessIssue> issues = new ArrayList<>();
        if (!validConfidence(metadata.confidence())) {
            issues.add(FreshnessIssue.INVALID_CONFIDENCE);
        }
        if (metadata.observedAt() == null || metadata.receivedAt() == null || metadata.expiresAt() == null) {
            issues.add(FreshnessIssue.MISSING_METADATA);
        } else {
            if (metadata.receivedAt().isBefore(metadata.observedAt())
                    || !metadata.expiresAt().isAfter(metadata.observedAt())) {
                issues.add(FreshnessIssue.INVALID_TEMPORAL_ORDER);
            }
            if (metadata.observedAt().isAfter(evaluationTime)
                    || metadata.receivedAt().isAfter(evaluationTime)) {
                issues.add(FreshnessIssue.FUTURE_OBSERVATION);
            }
        }
        if (metadata.status() == null || metadata.status() == ObservationStatus.UNVERIFIED) {
            issues.add(FreshnessIssue.SOURCE_UNVERIFIED);
        }
        if (!issues.isEmpty()) {
            return unverified(observation, normalizedConfidence(metadata.confidence()), issues);
        }

        if (metadata.status() == ObservationStatus.EXPIRED
                || !evaluationTime.isBefore(metadata.expiresAt())) {
            return new ObservationFreshnessAssessment(
                    observation.key(),
                    observation.requirement(),
                    ObservationStatus.EXPIRED,
                    false,
                    metadata.confidence(),
                    List.of(FreshnessIssue.EXPIRED)
            );
        }

        boolean belowMinimum = metadata.confidence() < policy.minimumConfirmedConfidence();
        if (belowMinimum && observation.requirement() == ObservationRequirement.HARD_FACT) {
            return unverified(
                    observation,
                    metadata.confidence(),
                    List.of(FreshnessIssue.BELOW_MINIMUM_CONFIDENCE)
            );
        }

        Duration remaining = Duration.between(evaluationTime, metadata.expiresAt());
        ObservationStatus status = metadata.status() == ObservationStatus.AGING
                || remaining.compareTo(policy.agingWindow()) <= 0
                ? ObservationStatus.AGING
                : ObservationStatus.FRESH;
        List<FreshnessIssue> advisoryIssues = belowMinimum
                ? List.of(FreshnessIssue.BELOW_MINIMUM_CONFIDENCE)
                : List.of();
        return new ObservationFreshnessAssessment(
                observation.key(),
                observation.requirement(),
                status,
                true,
                metadata.confidence(),
                advisoryIssues
        );
    }

    private ObservationFreshnessAssessment unverified(
            DecisionObservation observation,
            double confidence,
            List<FreshnessIssue> issues
    ) {
        return new ObservationFreshnessAssessment(
                observation.key(),
                observation.requirement(),
                ObservationStatus.UNVERIFIED,
                false,
                confidence,
                issues
        );
    }

    private static boolean validConfidence(double confidence) {
        return Double.isFinite(confidence) && confidence >= 0 && confidence <= 1;
    }

    private static double normalizedConfidence(double confidence) {
        return validConfidence(confidence) ? confidence : 0;
    }
}
