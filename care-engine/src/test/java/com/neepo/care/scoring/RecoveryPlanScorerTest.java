package com.neepo.care.scoring;

import com.neepo.care.context.ContextFreshnessPolicy;
import com.neepo.care.context.ContextFreshnessValidator;
import com.neepo.care.detection.DetectedDisruption;
import com.neepo.care.evaluation.HardConstraintEvaluationPolicy;
import com.neepo.care.evaluation.HardConstraintEvaluationResult;
import com.neepo.care.evaluation.HardConstraintEvaluator;
import com.neepo.care.evaluation.HardConstraintPlanEvaluation;
import com.neepo.care.planning.RecoveryPlanGenerationPolicy;
import com.neepo.care.planning.RecoveryPlanGenerator;
import com.neepo.contracts.CargoContext;
import com.neepo.contracts.CargoType;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import com.neepo.contracts.DriverContext;
import com.neepo.contracts.DriverStatus;
import com.neepo.contracts.GeoPoint;
import com.neepo.contracts.NetworkState;
import com.neepo.contracts.ObservationMetadata;
import com.neepo.contracts.ObservationStatus;
import com.neepo.contracts.PlanEvaluation;
import com.neepo.contracts.ProviderAvailabilityStatus;
import com.neepo.contracts.ProviderContext;
import com.neepo.contracts.ProviderType;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanType;
import com.neepo.contracts.RouteContext;
import com.neepo.contracts.RouteStatus;
import com.neepo.contracts.SafeStopContext;
import com.neepo.contracts.Trip;
import com.neepo.contracts.TripContext;
import com.neepo.contracts.TripStatus;
import com.neepo.contracts.VehicleContext;
import com.neepo.contracts.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryPlanScorerTest {
    private static final Instant NOW = Instant.parse("2026-08-20T12:00:00Z");
    private static final String HASH = "f".repeat(64);
    private static final UUID TRIP_ID = id("00000000-0000-4000-8600-000000000001");
    private static final UUID DISRUPTION_ID = id("00000000-0000-4000-9000-000000000001");
    private static final UUID FAST_PROVIDER = id("00000000-0000-4000-8700-000000000010");
    private static final UUID SLOW_PROVIDER = id("00000000-0000-4000-8700-000000000020");
    private static final UUID MOBILE_PROVIDER = id("00000000-0000-4000-8700-000000000030");
    private static final UUID TOWING_PROVIDER = id("00000000-0000-4000-8700-000000000040");
    private static final UUID SAFE_STOP = id("00000000-0000-4000-8800-000000000001");

    private final RecoveryPlanGenerator generator = new RecoveryPlanGenerator(
            RecoveryPlanGenerationPolicy.demoDefaults()
    );
    private final HardConstraintEvaluator hardEvaluator = new HardConstraintEvaluator(
            new ContextFreshnessValidator(ContextFreshnessPolicy.defaults()),
            HardConstraintEvaluationPolicy.demoDefaults()
    );
    private final SoftConstraintScoringPolicy policy = SoftConstraintScoringPolicy.demoDefaults();
    private final RecoveryPlanScorer scorer = new RecoveryPlanScorer(policy);

    @Test
    void feasibleCandidateProducesCompleteSharedPlanEvaluation() {
        TripContext context = context(List.of(fastProvider()), 100, NOW.plus(Duration.ofHours(10)), 60, 0.95);

        RecoveryPlan plan = plan(scored(context), FAST_PROVIDER);
        PlanEvaluation evaluation = plan.evaluation();

        assertEquals(RecoveryPlanStatus.FEASIBLE, plan.status());
        assertTrue(evaluation.feasible());
        assertTrue(evaluation.violatedHardConstraints().isEmpty());
        assertEquals(7, evaluation.softConstraintResults().size());
        assertEquals(4, evaluation.riskScore().scale());
        assertEquals(4, evaluation.uncertaintyScore().scale());
        assertEquals(4, evaluation.benefitScore().scale());
        assertEquals(4, evaluation.finalUtility().scale());
        assertEquals(policy.lambdaCoefficient(), evaluation.lambdaCoefficient());
        assertEquals(policy.muCoefficient(), evaluation.muCoefficient());
        assertEquals(RecoveryPlanScorer.EVALUATOR_VERSION, evaluation.evaluatorVersion());
        assertEquals(NOW, evaluation.evaluatedAt());
        assertTrue(evaluation.inputHash().matches("[a-f0-9]{64}"));
        assertEquals(0, expectedUtility(evaluation).compareTo(evaluation.finalUtility()));
        assertThrows(UnsupportedOperationException.class, () ->
                evaluation.softConstraintResults().put("MUTATION", BigDecimal.ONE));
    }

    @Test
    void lowerEtaAndHigherReliabilityProduceHigherUtility() {
        TripContext context = context(
                List.of(slowProvider(), fastProvider()),
                100,
                NOW.plus(Duration.ofHours(10)),
                60,
                0.95
        );

        RecoveryPlanScoringResult result = scored(context);
        RecoveryPlan fast = plan(result, FAST_PROVIDER);
        RecoveryPlan slow = plan(result, SLOW_PROVIDER);

        assertTrue(metric(fast, SoftConstraintScoringPolicy.PROVIDER_ETA_PREFERENCE)
                .compareTo(metric(slow, SoftConstraintScoringPolicy.PROVIDER_ETA_PREFERENCE)) > 0);
        assertTrue(metric(fast, SoftConstraintScoringPolicy.PROVIDER_RELIABILITY)
                .compareTo(metric(slow, SoftConstraintScoringPolicy.PROVIDER_RELIABILITY)) > 0);
        assertTrue(fast.evaluation().benefitScore().compareTo(slow.evaluation().benefitScore()) > 0);
        assertTrue(fast.evaluation().finalUtility().compareTo(slow.evaluation().finalUtility()) > 0);
    }

    @Test
    void rejectedCandidateCannotReceiveSoftPreferenceCredit() {
        ProviderContext unavailable = provider(
                FAST_PROVIDER,
                "Unavailable Workshop",
                20,
                decimal("95"),
                ProviderAvailabilityStatus.UNAVAILABLE,
                0.95,
                NOW.plus(Duration.ofHours(2))
        );
        TripContext context = context(
                List.of(unavailable), 100, NOW.plus(Duration.ofHours(10)), 60, 0.95
        );

        RecoveryPlan plan = plan(scored(context), FAST_PROVIDER);
        PlanEvaluation evaluation = plan.evaluation();

        assertEquals(RecoveryPlanStatus.REJECTED, plan.status());
        assertFalse(evaluation.feasible());
        assertTrue(evaluation.softConstraintResults().isEmpty());
        assertEquals(0, evaluation.riskScore().compareTo(BigDecimal.ZERO));
        assertEquals(0, evaluation.uncertaintyScore().compareTo(BigDecimal.ZERO));
        assertEquals(0, evaluation.benefitScore().compareTo(BigDecimal.ZERO));
        assertEquals(0, evaluation.finalUtility().compareTo(BigDecimal.ZERO));
        assertTrue(evaluation.violatedHardConstraints().contains(
                HardConstraintEvaluator.PROVIDER_FRESHNESS_RULE));
        assertTrue(evaluation.explanationCodes().contains(HardConstraintEvaluator.PROVIDER_UNAVAILABLE));
    }

    @Test
    void emergencyFallbackHasExplicitWorstRiskUtility() {
        TripContext context = context(List.of(fastProvider()), 100, NOW.plus(Duration.ofHours(10)), 60, 0.95);

        RecoveryPlan emergency = scored(context).plans().stream()
                .filter(plan -> plan.type() == RecoveryPlanType.EMERGENCY_ESCALATION)
                .findFirst()
                .orElseThrow();

        assertEquals(RecoveryPlanStatus.FEASIBLE, emergency.status());
        assertTrue(emergency.evaluation().softConstraintResults().isEmpty());
        assertEquals(0, emergency.evaluation().benefitScore().compareTo(decimal("0.0000")));
        assertEquals(0, emergency.evaluation().riskScore().compareTo(decimal("100.0000")));
        assertEquals(0, emergency.evaluation().uncertaintyScore().compareTo(decimal("100.0000")));
        assertEquals(0, emergency.evaluation().finalUtility().compareTo(decimal("-55.0000")));
    }

    @Test
    void etaAtWarningRatioBoundaryEmitsApprovedWarning() {
        ProviderContext boundary = provider(
                FAST_PROVIDER,
                "Boundary ETA Workshop",
                75,
                decimal("90"),
                ProviderAvailabilityStatus.AVAILABLE,
                0.95,
                NOW.plus(Duration.ofHours(2))
        );
        TripContext context = context(
                List.of(boundary), 100, NOW.plus(Duration.ofHours(10)), 60, 0.95
        );

        RecoveryPlan plan = plan(scored(context), FAST_PROVIDER);

        assertTrue(plan.evaluation().feasible());
        assertEquals(List.of(RecoveryPlanScorer.PROVIDER_ETA_HIGH), plan.evaluation().warnings());
        assertEquals(0, metric(plan, SoftConstraintScoringPolicy.PROVIDER_ETA_PREFERENCE)
                .compareTo(decimal("25.0000")));
    }

    @Test
    void normalizationClampsConfirmedPreferenceBoundariesToOneHundred() {
        ProviderContext ideal = provider(
                FAST_PROVIDER,
                "Ideal Workshop",
                0,
                decimal("100"),
                ProviderAvailabilityStatus.AVAILABLE,
                1.0,
                NOW.plus(Duration.ofHours(2))
        );
        TripContext context = context(
                List.of(ideal), 100, NOW.plus(Duration.ofHours(20)), 0, 1.0
        );

        RecoveryPlan plan = plan(scored(context), FAST_PROVIDER);

        assertEquals(0, metric(plan, SoftConstraintScoringPolicy.PROVIDER_ETA_PREFERENCE)
                .compareTo(decimal("100.0000")));
        assertEquals(0, metric(plan, SoftConstraintScoringPolicy.PROVIDER_RELIABILITY)
                .compareTo(decimal("100.0000")));
        assertEquals(0, metric(plan, SoftConstraintScoringPolicy.OBSERVATION_CONFIDENCE)
                .compareTo(decimal("100.0000")));
        assertEquals(0, metric(plan, SoftConstraintScoringPolicy.FRESHNESS_MARGIN)
                .compareTo(decimal("100.0000")));
        assertEquals(0, metric(plan, SoftConstraintScoringPolicy.DELIVERY_MARGIN)
                .compareTo(decimal("100.0000")));
    }

    @Test
    void repeatedAndReversedHardInputsProduceIdenticalOrderedResults() {
        TripContext context = context(
                List.of(slowProvider(), fastProvider()),
                100,
                NOW.plus(Duration.ofHours(10)),
                60,
                0.95
        );
        HardConstraintEvaluationResult hard = hard(context);
        List<HardConstraintPlanEvaluation> reversedPlans = new ArrayList<>(hard.planEvaluations());
        Collections.reverse(reversedPlans);

        RecoveryPlanScoringResult first = scorer.score(context, hard);
        RecoveryPlanScoringResult second = scorer.score(
                context,
                new HardConstraintEvaluationResult(reversedPlans)
        );

        assertEquals(first, second);
    }

    @Test
    void policyChangeChangesEvaluationHashAndUtility() {
        TripContext context = context(List.of(fastProvider()), 100, NOW.plus(Duration.ofHours(10)), 60, 0.95);
        HardConstraintEvaluationResult hard = hard(context);
        RecoveryPlan baseline = plan(scorer.score(context, hard), FAST_PROVIDER);
        SoftConstraintScoringPolicy changedPolicy = new SoftConstraintScoringPolicy(
                policy.recoveryDurationCeiling(),
                policy.executionComplexityCeiling(),
                policy.executionComplexityPoints(),
                policy.freshnessMarginCeiling(),
                policy.deliveryMarginCeiling(),
                policy.providerEtaHighRatio(),
                decimal("0.50"),
                policy.muCoefficient(),
                policy.benefitWeights(),
                policy.riskWeights(),
                policy.uncertaintyWeights()
        );

        RecoveryPlan changed = plan(
                new RecoveryPlanScorer(changedPolicy).score(context, hard),
                FAST_PROVIDER
        );

        assertNotEquals(baseline.evaluation().inputHash(), changed.evaluation().inputHash());
        assertTrue(changed.evaluation().finalUtility().compareTo(baseline.evaluation().finalUtility()) < 0);
    }

    @Test
    void scoringPolicyRejectsWeightsThatDoNotTotalOne() {
        assertThrows(IllegalArgumentException.class, () -> new SoftConstraintScoringPolicy(
                Duration.ofHours(4),
                6,
                policy.executionComplexityPoints(),
                Duration.ofHours(1),
                Duration.ofHours(12),
                decimal("0.75"),
                decimal("0.35"),
                decimal("0.20"),
                Map.of(SoftConstraintScoringPolicy.PROVIDER_RELIABILITY, decimal("0.50")),
                policy.riskWeights(),
                policy.uncertaintyWeights()
        ));
    }

    @Test
    void demoComplexityPolicyRanksCompatibleWorkshopAboveMobileAndTowing() {
        ObservationMetadata observation = observation(0.95, NOW.plus(Duration.ofMinutes(10)));
        ProviderContext workshop = provider(
                FAST_PROVIDER,
                "Workshop B - Compatible",
                ProviderType.WORKSHOP,
                List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR"),
                45,
                decimal("94"),
                observation
        );
        ProviderContext mobile = provider(
                MOBILE_PROVIDER,
                "Mobile Mechanic D - Replanning Fallback",
                ProviderType.MOBILE_MECHANIC,
                List.of("MOBILE_ENGINE_COOLING_REPAIR"),
                55,
                decimal("91"),
                observation
        );
        ProviderContext towing = provider(
                TOWING_PROVIDER,
                "Towing Service E",
                ProviderType.TOWING_SERVICE,
                List.of("HEAVY_VEHICLE_TOWING"),
                60,
                decimal("89"),
                observation
        );
        SafeStopContext safeStop = new SafeStopContext(
                SAFE_STOP,
                "Safe Parking North",
                new GeoPoint(11.45, 77.68),
                decimal("5"),
                decimal("25000"),
                true,
                observation
        );
        TripContext context = context(
                List.of(towing, mobile, workshop),
                List.of(safeStop),
                180,
                NOW.plus(Duration.ofHours(20)),
                360,
                0.95
        );

        List<RecoveryPlan> ranked = scored(context).feasiblePlans().stream()
                .filter(plan -> plan.type() != RecoveryPlanType.EMERGENCY_ESCALATION)
                .sorted((left, right) -> right.evaluation().finalUtility()
                        .compareTo(left.evaluation().finalUtility()))
                .toList();

        assertEquals(RecoveryPlanType.WORKSHOP_REPAIR, ranked.getFirst().type());
        assertEquals(FAST_PROVIDER, ranked.getFirst().providerId());
        assertTrue(ranked.getFirst().evaluation().finalUtility()
                .compareTo(plan(scored(context), MOBILE_PROVIDER).evaluation().finalUtility()) > 0);
        assertTrue(ranked.getFirst().evaluation().finalUtility()
                .compareTo(plan(scored(context), TOWING_PROVIDER).evaluation().finalUtility()) > 0);
    }

    @Test
    void noPerPlanRouteDetourMetricIsInvented() {
        TripContext context = context(List.of(fastProvider()), 100, NOW.plus(Duration.ofHours(10)), 60, 0.95);

        RecoveryPlan plan = plan(scored(context), FAST_PROVIDER);

        assertFalse(plan.evaluation().softConstraintResults().containsKey("ROUTE_DETOUR_PREFERENCE"));
        assertFalse(plan.evaluation().warnings().contains("ROUTE_DETOUR_HIGH"));
    }

    @Test
    void outOfContractReliabilityCannotReceiveMaximumPreference() {
        ProviderContext invalidReliability = provider(
                FAST_PROVIDER,
                "Invalid Reliability Workshop",
                20,
                decimal("101"),
                ProviderAvailabilityStatus.AVAILABLE,
                0.95,
                NOW.plus(Duration.ofHours(2))
        );
        TripContext context = context(
                List.of(invalidReliability), 100, NOW.plus(Duration.ofHours(10)), 60, 0.95
        );

        RecoveryPlan plan = plan(scored(context), FAST_PROVIDER);

        assertEquals(0, metric(plan, SoftConstraintScoringPolicy.PROVIDER_RELIABILITY)
                .compareTo(decimal("0.0000")));
    }

    private RecoveryPlanScoringResult scored(TripContext context) {
        return scorer.score(context, hard(context));
    }

    private HardConstraintEvaluationResult hard(TripContext context) {
        List<RecoveryPlan> generated = generator.generate(
                context,
                DISRUPTION_ID,
                overheating(context)
        ).plans();
        return hardEvaluator.evaluate(context, generated);
    }

    private static RecoveryPlan plan(RecoveryPlanScoringResult result, UUID providerId) {
        return result.plans().stream()
                .filter(plan -> providerId.equals(plan.providerId()))
                .findFirst()
                .orElseThrow();
    }

    private static BigDecimal metric(RecoveryPlan plan, String metric) {
        return plan.evaluation().softConstraintResults().get(metric);
    }

    private static BigDecimal expectedUtility(PlanEvaluation evaluation) {
        return evaluation.benefitScore()
                .subtract(evaluation.lambdaCoefficient().multiply(evaluation.riskScore()))
                .subtract(evaluation.muCoefficient().multiply(evaluation.uncertaintyScore()))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private static DetectedDisruption overheating(TripContext context) {
        return new DetectedDisruption(
                DisruptionType.VEHICLE_OVERHEATING,
                DisruptionSeverity.HIGH,
                context.evaluationTime(),
                List.of(),
                Map.of("contextHash", context.contextHash(), "ruleId", "VEHICLE-OVERHEAT-DETECT-001")
        );
    }

    private static TripContext context(
            List<ProviderContext> providers,
            int maximumWaitingMinutes,
            Instant deadline,
            int remainingRouteMinutes,
            double coreConfidence
    ) {
        return context(
                providers,
                List.of(),
                maximumWaitingMinutes,
                deadline,
                remainingRouteMinutes,
                coreConfidence
        );
    }

    private static TripContext context(
            List<ProviderContext> providers,
            List<SafeStopContext> safeStops,
            int maximumWaitingMinutes,
            Instant deadline,
            int remainingRouteMinutes,
            double coreConfidence
    ) {
        ObservationMetadata coreObservation = observation(
                coreConfidence,
                NOW.plus(Duration.ofHours(2))
        );
        UUID vehicleId = id("00000000-0000-4000-8300-000000000001");
        return new TripContext(
                new Trip(
                        TRIP_ID,
                        id("00000000-0000-4000-8000-000000000001"),
                        vehicleId,
                        id("00000000-0000-4000-8200-000000000001"),
                        id("00000000-0000-4000-8400-000000000001"),
                        id("00000000-0000-4000-8500-000000000001"),
                        "TRIP-SCORING-001",
                        TripStatus.DISRUPTED,
                        deadline,
                        maximumWaitingMinutes,
                        90,
                        NOW.minusSeconds(600),
                        NOW.minusSeconds(700),
                        NOW.minusSeconds(5)
                ),
                new VehicleContext(
                        vehicleId,
                        "TN-NEEPO-01",
                        "REFRIGERATED_TRUCK",
                        VehicleStatus.DISRUPTED,
                        decimal("3.80"),
                        decimal("2.50"),
                        decimal("14000"),
                        decimal("7000"),
                        decimal("105"),
                        decimal("112"),
                        decimal("92"),
                        true,
                        List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR"),
                        coreObservation
                ),
                new DriverContext(
                        id("00000000-0000-4000-8200-000000000001"),
                        DriverStatus.DRIVING,
                        360,
                        540,
                        NOW.minusSeconds(3600),
                        coreObservation
                ),
                new CargoContext(
                        id("00000000-0000-4000-8400-000000000001"),
                        CargoType.REFRIGERATED,
                        decimal("2500"),
                        decimal("2"),
                        decimal("8"),
                        decimal("6"),
                        false,
                        List.of("MAINTAIN_TEMPERATURE"),
                        coreObservation
                ),
                new RouteContext(
                        id("00000000-0000-4000-8500-000000000001"),
                        RouteStatus.ACTIVE,
                        new GeoPoint(11.3410, 77.7172),
                        new GeoPoint(12.9716, 77.5946),
                        new GeoPoint(11.4, 77.65),
                        decimal("260"),
                        remainingRouteMinutes,
                        List.of(),
                        coreObservation
                ),
                providers,
                safeStops,
                List.of(),
                NetworkState.ONLINE,
                List.of(),
                null,
                NOW,
                HASH
        );
    }

    private static ProviderContext fastProvider() {
        return provider(
                FAST_PROVIDER,
                "Fast Reliable Workshop",
                20,
                decimal("95"),
                ProviderAvailabilityStatus.AVAILABLE,
                0.95,
                NOW.plus(Duration.ofHours(2))
        );
    }

    private static ProviderContext slowProvider() {
        return provider(
                SLOW_PROVIDER,
                "Slow Workshop",
                60,
                decimal("80"),
                ProviderAvailabilityStatus.AVAILABLE,
                0.95,
                NOW.plus(Duration.ofHours(2))
        );
    }

    private static ProviderContext provider(
            UUID providerId,
            String name,
            int etaMinutes,
            BigDecimal reliability,
            ProviderAvailabilityStatus availability,
            double confidence,
            Instant expiresAt
    ) {
        return provider(
                providerId,
                name,
                ProviderType.WORKSHOP,
                List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR"),
                etaMinutes,
                reliability,
                observation(confidence, expiresAt),
                availability
        );
    }

    private static ProviderContext provider(
            UUID providerId,
            String name,
            ProviderType type,
            List<String> capabilities,
            int etaMinutes,
            BigDecimal reliability,
            ObservationMetadata observation
    ) {
        return provider(
                providerId,
                name,
                type,
                capabilities,
                etaMinutes,
                reliability,
                observation,
                ProviderAvailabilityStatus.AVAILABLE
        );
    }

    private static ProviderContext provider(
            UUID providerId,
            String name,
            ProviderType type,
            List<String> capabilities,
            int etaMinutes,
            BigDecimal reliability,
            ObservationMetadata observation,
            ProviderAvailabilityStatus availability
    ) {
        return new ProviderContext(
                providerId,
                name,
                type,
                new GeoPoint(11.5, 77.7),
                capabilities,
                availability,
                etaMinutes,
                reliability,
                true,
                observation
        );
    }

    private static ObservationMetadata observation(double confidence, Instant expiresAt) {
        return new ObservationMetadata(
                NOW.minusSeconds(30),
                NOW.minusSeconds(29),
                expiresAt,
                confidence,
                ObservationStatus.FRESH,
                "SIMULATOR"
        );
    }

    private static UUID id(String value) {
        return UUID.fromString(value);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
