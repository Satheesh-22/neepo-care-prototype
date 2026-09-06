package com.neepo.care.scoring;

import com.neepo.contracts.RecoveryPlanType;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public record SoftConstraintScoringPolicy(
        Duration recoveryDurationCeiling,
        int executionComplexityCeiling,
        Map<RecoveryPlanType, Integer> executionComplexityPoints,
        Duration freshnessMarginCeiling,
        Duration deliveryMarginCeiling,
        BigDecimal providerEtaHighRatio,
        BigDecimal lambdaCoefficient,
        BigDecimal muCoefficient,
        Map<String, BigDecimal> benefitWeights,
        Map<String, BigDecimal> riskWeights,
        Map<String, BigDecimal> uncertaintyWeights
) {
    public static final String PROVIDER_ETA_PREFERENCE = "PROVIDER_ETA_PREFERENCE";
    public static final String RECOVERY_DURATION_PREFERENCE = "RECOVERY_DURATION_PREFERENCE";
    public static final String EXECUTION_SIMPLICITY = "EXECUTION_SIMPLICITY";
    public static final String PROVIDER_RELIABILITY = "PROVIDER_RELIABILITY";
    public static final String OBSERVATION_CONFIDENCE = "OBSERVATION_CONFIDENCE";
    public static final String FRESHNESS_MARGIN = "FRESHNESS_MARGIN";
    public static final String DELIVERY_MARGIN = "DELIVERY_MARGIN";

    private static final Set<String> METRICS = Set.of(
            PROVIDER_ETA_PREFERENCE,
            RECOVERY_DURATION_PREFERENCE,
            EXECUTION_SIMPLICITY,
            PROVIDER_RELIABILITY,
            OBSERVATION_CONFIDENCE,
            FRESHNESS_MARGIN,
            DELIVERY_MARGIN
    );
    private static final Set<RecoveryPlanType> SCORABLE_PLAN_TYPES = Set.of(
            RecoveryPlanType.WORKSHOP_REPAIR,
            RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC,
            RecoveryPlanType.TOW_REPAIR_OR_TRANSFER,
            RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER
    );

    public SoftConstraintScoringPolicy {
        requirePositive(recoveryDurationCeiling, "Recovery duration ceiling");
        requirePositive(freshnessMarginCeiling, "Freshness margin ceiling");
        requirePositive(deliveryMarginCeiling, "Delivery margin ceiling");
        if (executionComplexityCeiling <= 1) {
            throw new IllegalArgumentException("Execution complexity ceiling must exceed one point");
        }
        executionComplexityPoints = validatedComplexity(
                executionComplexityPoints,
                executionComplexityCeiling
        );
        requireBetweenZeroAndOne(providerEtaHighRatio, "Provider ETA warning ratio");
        requireNonNegative(lambdaCoefficient, "Lambda coefficient");
        requireNonNegative(muCoefficient, "Mu coefficient");
        benefitWeights = validatedWeights("Benefit", benefitWeights);
        riskWeights = validatedWeights("Risk", riskWeights);
        uncertaintyWeights = validatedWeights("Uncertainty", uncertaintyWeights);
    }

    public static SoftConstraintScoringPolicy demoDefaults() {
        return new SoftConstraintScoringPolicy(
                Duration.ofHours(4),
                6,
                Map.of(
                        RecoveryPlanType.WORKSHOP_REPAIR, 2,
                        RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC, 4,
                        RecoveryPlanType.TOW_REPAIR_OR_TRANSFER, 6,
                        RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER, 6
                ),
                Duration.ofHours(1),
                Duration.ofHours(12),
                decimal("0.75"),
                decimal("0.35"),
                decimal("0.20"),
                Map.of(
                        PROVIDER_RELIABILITY, decimal("0.35"),
                        PROVIDER_ETA_PREFERENCE, decimal("0.25"),
                        RECOVERY_DURATION_PREFERENCE, decimal("0.20"),
                        DELIVERY_MARGIN, decimal("0.20")
                ),
                Map.of(
                        RECOVERY_DURATION_PREFERENCE, decimal("0.25"),
                        EXECUTION_SIMPLICITY, decimal("0.60"),
                        DELIVERY_MARGIN, decimal("0.15")
                ),
                Map.of(
                        OBSERVATION_CONFIDENCE, decimal("0.60"),
                        FRESHNESS_MARGIN, decimal("0.40")
                )
        );
    }

    String canonicalForm() {
        StringBuilder value = new StringBuilder();
        value.append(recoveryDurationCeiling)
                .append('|').append(executionComplexityCeiling)
                .append('|').append(freshnessMarginCeiling)
                .append('|').append(deliveryMarginCeiling)
                .append('|').append(providerEtaHighRatio.toPlainString())
                .append('|').append(lambdaCoefficient.toPlainString())
                .append('|').append(muCoefficient.toPlainString());
        executionComplexityPoints.forEach((type, points) -> value
                .append("|complexity.").append(type.name()).append('=').append(points));
        appendWeights(value, "benefit", benefitWeights);
        appendWeights(value, "risk", riskWeights);
        appendWeights(value, "uncertainty", uncertaintyWeights);
        return value.toString();
    }

    public int executionComplexity(RecoveryPlanType type) {
        Integer points = executionComplexityPoints.get(type);
        if (points == null) {
            throw new IllegalArgumentException("No execution complexity configured for " + type);
        }
        return points;
    }

    private static Map<RecoveryPlanType, Integer> validatedComplexity(
            Map<RecoveryPlanType, Integer> values,
            int ceiling
    ) {
        if (values == null || !values.keySet().containsAll(SCORABLE_PLAN_TYPES)) {
            throw new IllegalArgumentException("Execution complexity is required for every normal plan type");
        }
        EnumMap<RecoveryPlanType, Integer> ordered = new EnumMap<>(RecoveryPlanType.class);
        values.forEach((type, points) -> {
            if (type == null || points == null || points < 1 || points > ceiling) {
                throw new IllegalArgumentException("Execution complexity points must be within the configured ceiling");
            }
            ordered.put(type, points);
        });
        return Collections.unmodifiableMap(ordered);
    }

    private static Map<String, BigDecimal> validatedWeights(
            String label,
            Map<String, BigDecimal> weights
    ) {
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException(label + " weights are required");
        }
        TreeMap<String, BigDecimal> ordered = new TreeMap<>();
        weights.forEach((metric, weight) -> {
            if (!METRICS.contains(metric)) {
                throw new IllegalArgumentException("Unknown soft metric: " + metric);
            }
            requireNonNegative(weight, label + " weight for " + metric);
            ordered.put(metric, weight);
        });
        BigDecimal total = ordered.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException(label + " weights must total exactly one");
        }
        return Collections.unmodifiableMap(ordered);
    }

    private static void appendWeights(
            StringBuilder value,
            String label,
            Map<String, BigDecimal> weights
    ) {
        weights.forEach((metric, weight) -> value
                .append('|').append(label).append('.')
                .append(metric).append('=').append(weight.toPlainString()));
    }

    private static void requirePositive(Duration value, String label) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }

    private static void requireBetweenZeroAndOne(BigDecimal value, String label) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(label + " must be between zero and one");
        }
    }

    private static void requireNonNegative(BigDecimal value, String label) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + " must not be negative");
        }
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
