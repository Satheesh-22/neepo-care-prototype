package com.neepo.care.benchmark;

import com.neepo.care.evaluation.HardConstraintEvaluator;
import com.neepo.care.selection.RecoveryPlanSelectionResult;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanType;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PrototypeBenchmarkRunner {
    public static final String CARE_STRATEGY = "CARE_ADAPTIVE";
    public static final String BASELINE_STRATEGY = "NEAREST_PROVIDER_FIXED_TELEMETRY";

    private static final List<String> STALE_CODES = List.of(
            HardConstraintEvaluator.PROVIDER_AVAILABILITY_EXPIRED,
            HardConstraintEvaluator.CONTEXT_OBSERVATION_EXPIRED
    );

    public PrototypeBenchmarkReport run(
            BenchmarkMetadata metadata,
            RecoveryPlanSelectionResult careSelection,
            Map<UUID, Integer> providerEtaMinutes,
            TelemetryBenchmarkComparison telemetry,
            int careDecisionLatencyMs,
            int baselineDecisionLatencyMs
    ) {
        Objects.requireNonNull(metadata, "Benchmark metadata is required");
        Objects.requireNonNull(careSelection, "CARE selection is required");
        Objects.requireNonNull(telemetry, "Telemetry comparison is required");
        if (careDecisionLatencyMs < 0 || baselineDecisionLatencyMs < 0) {
            throw new IllegalArgumentException("Measured decision latencies must be nonnegative");
        }
        Map<UUID, Integer> etas = validatedEtas(providerEtaMinutes);
        RecoveryPlan carePlan = careSelection.selectedPlan();
        RecoveryPlan nearest = nearestProviderPlan(careSelection.plans(), etas);

        StrategyMetrics care = metrics(
                CARE_STRATEGY,
                carePlan,
                telemetry.adaptiveTransmissions(),
                telemetry.adaptiveBandwidthBytes(),
                careDecisionLatencyMs,
                false
        );
        StrategyMetrics baseline = metrics(
                BASELINE_STRATEGY,
                nearest,
                telemetry.fixedTransmissions(),
                telemetry.fixedBandwidthBytes(),
                baselineDecisionLatencyMs,
                true
        );

        LinkedHashMap<String, String> configuration = new LinkedHashMap<>();
        configuration.put("fixedTelemetryIntervalSeconds", String.valueOf(telemetry.fixedIntervalSeconds()));
        configuration.put("telemetryPayloadBytes", String.valueOf(telemetry.payloadBytes()));
        configuration.put("nearestProviderTieBreak", "ETA_ASC_THEN_PLAN_UUID_ASC");
        configuration.put("latencySource", "MEASURED_INPUT");
        return new PrototypeBenchmarkReport(metadata, configuration, List.of(care, baseline));
    }

    private static StrategyMetrics metrics(
            String strategy,
            RecoveryPlan plan,
            int transmissions,
            long bandwidth,
            int latencyMs,
            boolean baseline
    ) {
        boolean emergency = plan.type() == RecoveryPlanType.EMERGENCY_ESCALATION;
        boolean valid = plan.evaluation() != null && plan.evaluation().feasible();
        int invalidRecommendations = valid ? 0 : 1;
        int staleDecisions = baseline && plan.evaluation() != null
                && plan.evaluation().explanationCodes().stream().anyMatch(STALE_CODES::contains)
                ? 1
                : 0;
        return new StrategyMetrics(
                strategy,
                valid && !emergency,
                invalidRecommendations,
                recoveryTimeSeconds(plan),
                emergency ? 0 : 1,
                transmissions,
                bandwidth,
                latencyMs,
                staleDecisions
        );
    }

    private static RecoveryPlan nearestProviderPlan(
            List<RecoveryPlan> plans,
            Map<UUID, Integer> providerEtas
    ) {
        return plans.stream()
                .filter(plan -> plan.type() != RecoveryPlanType.EMERGENCY_ESCALATION)
                .filter(plan -> plan.providerId() != null && providerEtas.containsKey(plan.providerId()))
                .min(Comparator
                        .comparingInt((RecoveryPlan plan) -> providerEtas.get(plan.providerId()))
                        .thenComparing(plan -> plan.id().toString()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nearest-provider baseline requires at least one provider-bound normal plan"
                ));
    }

    private static int recoveryTimeSeconds(RecoveryPlan plan) {
        if (plan.steps() == null || plan.steps().isEmpty()
                || plan.generatedAt() == null
                || plan.steps().getLast().expectedCompleteAt() == null) {
            throw new IllegalArgumentException("Benchmark plan requires complete recovery timing");
        }
        long seconds = Duration.between(
                plan.generatedAt(),
                plan.steps().getLast().expectedCompleteAt()
        ).toSeconds();
        if (seconds < 0) {
            throw new IllegalArgumentException("Benchmark recovery duration cannot be negative");
        }
        return Math.toIntExact(seconds);
    }

    private static Map<UUID, Integer> validatedEtas(Map<UUID, Integer> providerEtaMinutes) {
        Objects.requireNonNull(providerEtaMinutes, "Provider ETA values are required");
        LinkedHashMap<UUID, Integer> result = new LinkedHashMap<>();
        providerEtaMinutes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)))
                .forEach(entry -> {
                    Objects.requireNonNull(entry.getKey(), "Provider ETA id is required");
                    if (entry.getValue() == null || entry.getValue() < 0) {
                        throw new IllegalArgumentException("Provider ETA must be nonnegative");
                    }
                    result.put(entry.getKey(), entry.getValue());
                });
        return Map.copyOf(result);
    }
}
