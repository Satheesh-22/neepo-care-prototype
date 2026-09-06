package com.neepo.care.scoring;

import com.neepo.care.context.DecisionObservation;
import com.neepo.care.context.DecisionState;
import com.neepo.care.context.ObservationRequirement;
import com.neepo.care.evaluation.HardConstraintEvaluationResult;
import com.neepo.care.evaluation.HardConstraintEvaluator;
import com.neepo.care.evaluation.HardConstraintPlanEvaluation;
import com.neepo.contracts.ObservationMetadata;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.ProviderContext;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanStep;
import com.neepo.contracts.RecoveryPlanType;
import com.neepo.contracts.SafeStopContext;
import com.neepo.contracts.TripContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public final class RecoveryPlanScorer {
    public static final String EVALUATOR_VERSION = "plan-evaluator-1.0.0";
    public static final String PROVIDER_ETA_HIGH = "PROVIDER_ETA_HIGH";

    private static final int NORMALIZATION_SCALE = 12;
    private static final int OUTPUT_SCALE = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO_SCORE = BigDecimal.ZERO.setScale(OUTPUT_SCALE);
    private static final BigDecimal MAXIMUM_SCORE = HUNDRED.setScale(OUTPUT_SCALE);
    private static final Comparator<HardConstraintPlanEvaluation> PLAN_ORDER = Comparator
            .comparingInt((HardConstraintPlanEvaluation evaluation) -> evaluation.plan().type().ordinal())
            .thenComparing(evaluation -> nullableId(evaluation.plan().providerId()))
            .thenComparing(evaluation -> nullableId(evaluation.plan().safeStopId()))
            .thenComparing(evaluation -> evaluation.plan().id().toString());

    private final SoftConstraintScoringPolicy policy;

    public RecoveryPlanScorer(SoftConstraintScoringPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "Soft-constraint scoring policy is required");
    }

    public RecoveryPlanScoringResult score(
            TripContext context,
            HardConstraintEvaluationResult hardResult
    ) {
        DecisionState state = DecisionState.from(context);
        Objects.requireNonNull(hardResult, "Hard-constraint evaluation result is required");
        Map<UUID, ProviderContext> providers = providersById(context.providers());
        Map<UUID, SafeStopContext> safeStops = safeStopsById(context.safeStops());
        validateHardEvaluations(state, hardResult.planEvaluations());

        List<RecoveryPlan> plans = hardResult.planEvaluations().stream()
                .sorted(PLAN_ORDER)
                .map(hard -> scoredPlan(context, state, providers, safeStops, hard))
                .toList();
        return new RecoveryPlanScoringResult(plans);
    }

    private RecoveryPlan scoredPlan(
            TripContext context,
            DecisionState state,
            Map<UUID, ProviderContext> providers,
            Map<UUID, SafeStopContext> safeStops,
            HardConstraintPlanEvaluation hard
    ) {
        RecoveryPlan plan = hard.plan();
        ProviderContext provider = plan.providerId() == null ? null : providers.get(plan.providerId());
        SafeStopContext safeStop = plan.safeStopId() == null ? null : safeStops.get(plan.safeStopId());

        Score score;
        if (!hard.feasible()) {
            score = new Score(Map.of(), ZERO_SCORE, ZERO_SCORE, ZERO_SCORE, ZERO_SCORE, List.of());
        } else if (plan.type() == RecoveryPlanType.EMERGENCY_ESCALATION) {
            BigDecimal utility = utility(ZERO_SCORE, MAXIMUM_SCORE, MAXIMUM_SCORE);
            score = new Score(
                    Map.of(),
                    MAXIMUM_SCORE,
                    MAXIMUM_SCORE,
                    ZERO_SCORE,
                    utility,
                    List.of()
            );
        } else {
            if (provider == null) {
                throw new IllegalArgumentException("Feasible normal plan must reference a context provider");
            }
            if (plan.type() == RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC && safeStop == null) {
                throw new IllegalArgumentException("Feasible mobile-mechanic plan must reference a context safe stop");
            }
            score = normalScore(context, state, plan, provider, safeStop, hard);
        }

        LinkedHashSet<String> warnings = new LinkedHashSet<>(hard.warnings());
        warnings.addAll(score.warnings());
        PlanEvaluation evaluation = new PlanEvaluation(
                plan.id(),
                hard.feasible(),
                hard.violatedHardConstraints(),
                List.copyOf(warnings),
                immutableMetrics(score.metrics()),
                score.risk(),
                score.uncertainty(),
                score.benefit(),
                policy.lambdaCoefficient(),
                policy.muCoefficient(),
                score.utility(),
                hard.confidence(),
                Collections.unmodifiableMap(new LinkedHashMap<>(hard.freshnessSummary())),
                hard.explanationCodes(),
                state.evaluationTime(),
                EVALUATOR_VERSION,
                evaluationInputHash(hard.inputHash())
        );
        return new RecoveryPlan(
                plan.id(),
                plan.tripId(),
                plan.disruptionId(),
                plan.type(),
                hard.feasible() ? RecoveryPlanStatus.FEASIBLE : RecoveryPlanStatus.REJECTED,
                plan.providerId(),
                plan.safeStopId(),
                List.copyOf(plan.steps()),
                evaluation,
                plan.generatedAt(),
                null
        );
    }

    private Score normalScore(
            TripContext context,
            DecisionState state,
            RecoveryPlan plan,
            ProviderContext provider,
            SafeStopContext safeStop,
            HardConstraintPlanEvaluation hard
    ) {
        TreeMap<String, BigDecimal> metrics = new TreeMap<>();
        metrics.put(
                SoftConstraintScoringPolicy.PROVIDER_ETA_PREFERENCE,
                etaPreference(context.trip().maximumWaitingMinutes(), provider.estimatedArrivalMinutes())
        );
        metrics.put(
                SoftConstraintScoringPolicy.RECOVERY_DURATION_PREFERENCE,
                durationPreference(planDuration(plan), policy.recoveryDurationCeiling())
        );
        metrics.put(
                SoftConstraintScoringPolicy.EXECUTION_SIMPLICITY,
                simplicityPreference(
                        policy.executionComplexity(plan.type()),
                        policy.executionComplexityCeiling()
                )
        );
        metrics.put(
                SoftConstraintScoringPolicy.PROVIDER_RELIABILITY,
                reliabilityPreference(provider.reliabilityScore())
        );
        metrics.put(
                SoftConstraintScoringPolicy.OBSERVATION_CONFIDENCE,
                normalizedScore(BigDecimal.valueOf(hard.confidence()).multiply(HUNDRED))
        );
        metrics.put(
                SoftConstraintScoringPolicy.FRESHNESS_MARGIN,
                freshnessPreference(context, state, provider, safeStop)
        );
        metrics.put(
                SoftConstraintScoringPolicy.DELIVERY_MARGIN,
                deliveryMarginPreference(context, plan, provider)
        );

        BigDecimal benefit = weightedScore(metrics, policy.benefitWeights(), false);
        BigDecimal risk = weightedScore(metrics, policy.riskWeights(), true);
        BigDecimal uncertainty = weightedScore(metrics, policy.uncertaintyWeights(), true);
        BigDecimal utility = utility(benefit, risk, uncertainty);

        List<String> warnings = providerEtaHigh(context, provider)
                ? List.of(PROVIDER_ETA_HIGH)
                : List.of();
        return new Score(metrics, risk, uncertainty, benefit, utility, warnings);
    }

    private BigDecimal etaPreference(int maximumWaitMinutes, Integer etaMinutes) {
        if (maximumWaitMinutes <= 0 || etaMinutes == null) {
            return ZERO_SCORE;
        }
        return percentage(
                BigDecimal.valueOf((long) maximumWaitMinutes - etaMinutes),
                BigDecimal.valueOf(maximumWaitMinutes)
        );
    }

    private BigDecimal durationPreference(Duration duration, Duration ceiling) {
        return percentage(
                BigDecimal.valueOf(ceiling.minus(duration).toSeconds()),
                BigDecimal.valueOf(ceiling.toSeconds())
        );
    }

    private BigDecimal simplicityPreference(int stepCount, int ceiling) {
        return percentage(
                BigDecimal.valueOf((long) ceiling - stepCount),
                BigDecimal.valueOf((long) ceiling - 1)
        );
    }

    private BigDecimal freshnessPreference(
            TripContext context,
            DecisionState state,
            ProviderContext provider,
            SafeStopContext safeStop
    ) {
        List<ObservationMetadata> critical = new ArrayList<>(state.observations().stream()
                .filter(observation -> observation.requirement() == ObservationRequirement.HARD_FACT)
                .map(DecisionObservation::metadata)
                .toList());
        critical.add(provider.availabilityObservation());
        if (safeStop != null) {
            critical.add(safeStop.observation());
        }
        long minimumRemainingSeconds = critical.stream()
                .map(metadata -> remainingSeconds(context.evaluationTime(), metadata))
                .min(Long::compareTo)
                .orElse(0L);
        return percentage(
                BigDecimal.valueOf(minimumRemainingSeconds),
                BigDecimal.valueOf(policy.freshnessMarginCeiling().toSeconds())
        );
    }

    private BigDecimal deliveryMarginPreference(
            TripContext context,
            RecoveryPlan plan,
            ProviderContext provider
    ) {
        Instant revisedArrival = plan.steps().getLast().expectedCompleteAt()
                .plus(Duration.ofMinutes((long) provider.estimatedArrivalMinutes()
                        + context.route().remainingDurationMinutes()));
        long marginSeconds = Duration.between(revisedArrival, context.trip().deliveryDeadline()).toSeconds();
        return percentage(
                BigDecimal.valueOf(marginSeconds),
                BigDecimal.valueOf(policy.deliveryMarginCeiling().toSeconds())
        );
    }

    private BigDecimal weightedScore(
            Map<String, BigDecimal> metrics,
            Map<String, BigDecimal> weights,
            boolean invert
    ) {
        BigDecimal score = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> weight : weights.entrySet()) {
            BigDecimal component = metrics.get(weight.getKey());
            if (component == null) {
                throw new IllegalStateException("Missing normalized soft metric: " + weight.getKey());
            }
            if (invert) {
                component = HUNDRED.subtract(component);
            }
            score = score.add(component.multiply(weight.getValue()));
        }
        return score.setScale(OUTPUT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal utility(BigDecimal benefit, BigDecimal risk, BigDecimal uncertainty) {
        return benefit
                .subtract(policy.lambdaCoefficient().multiply(risk))
                .subtract(policy.muCoefficient().multiply(uncertainty))
                .setScale(OUTPUT_SCALE, RoundingMode.HALF_UP);
    }

    private boolean providerEtaHigh(TripContext context, ProviderContext provider) {
        if (context.trip().maximumWaitingMinutes() <= 0 || provider.estimatedArrivalMinutes() == null) {
            return false;
        }
        BigDecimal ratio = BigDecimal.valueOf(provider.estimatedArrivalMinutes()).divide(
                BigDecimal.valueOf(context.trip().maximumWaitingMinutes()),
                NORMALIZATION_SCALE,
                RoundingMode.HALF_UP
        );
        return ratio.compareTo(policy.providerEtaHighRatio()) >= 0;
    }

    private static Duration planDuration(RecoveryPlan plan) {
        return Duration.between(plan.generatedAt(), plan.steps().getLast().expectedCompleteAt());
    }

    private static long remainingSeconds(Instant evaluationTime, ObservationMetadata metadata) {
        if (metadata == null || metadata.expiresAt() == null) {
            return 0;
        }
        return Math.max(Duration.between(evaluationTime, metadata.expiresAt()).toSeconds(), 0);
    }

    private static BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() <= 0) {
            return ZERO_SCORE;
        }
        BigDecimal value = numerator.multiply(HUNDRED).divide(
                denominator,
                NORMALIZATION_SCALE,
                RoundingMode.HALF_UP
        );
        return normalizedScore(value);
    }

    private static BigDecimal normalizedScore(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return ZERO_SCORE;
        }
        if (value.compareTo(HUNDRED) > 0) {
            return MAXIMUM_SCORE;
        }
        return value.setScale(OUTPUT_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal reliabilityPreference(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(HUNDRED) > 0) {
            return ZERO_SCORE;
        }
        return value.setScale(OUTPUT_SCALE, RoundingMode.HALF_UP);
    }

    private String evaluationInputHash(String hardInputHash) {
        return sha256(String.join("|", hardInputHash, EVALUATOR_VERSION, policy.canonicalForm()));
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte current : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", current));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for scoring input hashing", exception);
        }
    }

    private static Map<String, BigDecimal> immutableMetrics(Map<String, BigDecimal> metrics) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(new TreeMap<>(metrics)));
    }

    private static void validateHardEvaluations(
            DecisionState state,
            List<HardConstraintPlanEvaluation> evaluations
    ) {
        Objects.requireNonNull(evaluations, "Hard plan evaluations are required");
        Set<UUID> planIds = new HashSet<>();
        for (HardConstraintPlanEvaluation hard : evaluations) {
            Objects.requireNonNull(hard, "Hard plan evaluation entry is required");
            if (!HardConstraintEvaluator.EVALUATOR_VERSION.equals(hard.evaluatorVersion())) {
                throw new IllegalArgumentException("Unsupported hard-constraint evaluator version");
            }
            if (!state.tripId().equals(hard.plan().tripId())) {
                throw new IllegalArgumentException("Hard evaluation trip does not match the context trip");
            }
            if (!state.evaluationTime().equals(hard.evaluatedAt())) {
                throw new IllegalArgumentException("Hard evaluation time does not match context evaluation time");
            }
            if (hard.plan().status() != RecoveryPlanStatus.GENERATED
                    || hard.plan().evaluation() != null
                    || hard.plan().selectedAt() != null) {
                throw new IllegalArgumentException("Hard evaluation must reference an unevaluated generated plan");
            }
            if (!planIds.add(hard.plan().id())) {
                throw new IllegalArgumentException("Duplicate hard evaluation plan id: " + hard.plan().id());
            }
        }
    }

    private static Map<UUID, ProviderContext> providersById(List<ProviderContext> providers) {
        Map<UUID, ProviderContext> result = new HashMap<>();
        for (ProviderContext provider : providers == null ? List.<ProviderContext>of() : providers) {
            Objects.requireNonNull(provider, "Provider context entry is required");
            Objects.requireNonNull(provider.providerId(), "Provider id is required");
            if (result.put(provider.providerId(), provider) != null) {
                throw new IllegalArgumentException("Duplicate provider id: " + provider.providerId());
            }
        }
        return Map.copyOf(result);
    }

    private static Map<UUID, SafeStopContext> safeStopsById(List<SafeStopContext> safeStops) {
        Map<UUID, SafeStopContext> result = new HashMap<>();
        for (SafeStopContext safeStop : safeStops == null ? List.<SafeStopContext>of() : safeStops) {
            Objects.requireNonNull(safeStop, "Safe-stop context entry is required");
            Objects.requireNonNull(safeStop.safeStopId(), "Safe-stop id is required");
            if (result.put(safeStop.safeStopId(), safeStop) != null) {
                throw new IllegalArgumentException("Duplicate safe-stop id: " + safeStop.safeStopId());
            }
        }
        return Map.copyOf(result);
    }

    private static String nullableId(UUID value) {
        return value == null ? "-" : value.toString();
    }

    private record Score(
            Map<String, BigDecimal> metrics,
            BigDecimal risk,
            BigDecimal uncertainty,
            BigDecimal benefit,
            BigDecimal utility,
            List<String> warnings
    ) {
    }
}
