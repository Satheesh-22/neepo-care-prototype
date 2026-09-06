package com.neepo.care.evaluation;

import com.neepo.care.context.ContextFreshnessPolicy;
import com.neepo.care.context.ContextFreshnessValidator;
import com.neepo.care.detection.DetectedDisruption;
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
import com.neepo.contracts.ProviderAvailabilityStatus;
import com.neepo.contracts.ProviderContext;
import com.neepo.contracts.ProviderType;
import com.neepo.contracts.RecoveryPlan;
import com.neepo.contracts.RecoveryPlanStep;
import com.neepo.contracts.RecoveryPlanType;
import com.neepo.contracts.RouteContext;
import com.neepo.contracts.RouteRestriction;
import com.neepo.contracts.RouteStatus;
import com.neepo.contracts.SafeStopContext;
import com.neepo.contracts.Trip;
import com.neepo.contracts.TripContext;
import com.neepo.contracts.TripStatus;
import com.neepo.contracts.VehicleContext;
import com.neepo.contracts.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

class HardConstraintEvaluatorTest {
    private static final Instant NOW = Instant.parse("2026-08-20T12:00:00Z");
    private static final String HASH = "e".repeat(64);
    private static final UUID TRIP_ID = id("00000000-0000-4000-8600-000000000001");
    private static final UUID DISRUPTION_ID = id("00000000-0000-4000-9000-000000000001");
    private static final UUID WORKSHOP_A = id("00000000-0000-4000-8700-000000000001");
    private static final UUID WORKSHOP_B = id("00000000-0000-4000-8700-000000000002");
    private static final UUID WORKSHOP_C = id("00000000-0000-4000-8700-000000000003");
    private static final UUID MOBILE_MECHANIC = id("00000000-0000-4000-8700-000000000004");
    private static final UUID TOWING_SERVICE = id("00000000-0000-4000-8700-000000000005");
    private static final UUID SAFE_STOP_A = id("00000000-0000-4000-8800-000000000001");
    private static final UUID SAFE_STOP_B = id("00000000-0000-4000-8800-000000000002");

    private final RecoveryPlanGenerator generator = new RecoveryPlanGenerator(
            RecoveryPlanGenerationPolicy.demoDefaults()
    );
    private final HardConstraintEvaluator evaluator = new HardConstraintEvaluator(
            new ContextFreshnessValidator(ContextFreshnessPolicy.defaults()),
            HardConstraintEvaluationPolicy.demoDefaults()
    );

    @Test
    void demoCandidatesAreEvaluatedIndependentlyWithExpectedRejections() {
        TripContext context = demoContext();

        HardConstraintEvaluationResult result = evaluate(context);

        assertEquals(7, result.planEvaluations().size());
        assertEquals(5, result.feasiblePlans().size());
        assertEquals(2, result.rejectedPlans().size());

        HardConstraintPlanEvaluation nearest = evaluation(result, RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_A);
        assertEquals(List.of(
                HardConstraintEvaluator.VEHICLE_COMPATIBILITY_RULE,
                HardConstraintEvaluator.ROUTE_DIMENSION_RULE
        ), nearest.violatedHardConstraints());
        assertEquals(List.of(
                HardConstraintEvaluator.VEHICLE_TYPE_UNSUPPORTED,
                HardConstraintEvaluator.ROAD_HEIGHT_LIMIT_EXCEEDED
        ), nearest.explanationCodes());
        assertEquals(13, nearest.assessments().size());

        HardConstraintPlanEvaluation compatible = evaluation(
                result, RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_B
        );
        assertTrue(compatible.feasible());
        assertTrue(compatible.violatedHardConstraints().isEmpty());

        HardConstraintPlanEvaluation stale = evaluation(result, RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_C);
        assertEquals(List.of(
                HardConstraintEvaluator.PROVIDER_FRESHNESS_RULE,
                HardConstraintEvaluator.CONTEXT_FRESHNESS_RULE
        ), stale.violatedHardConstraints());
        assertEquals(List.of(
                HardConstraintEvaluator.PROVIDER_AVAILABILITY_EXPIRED,
                HardConstraintEvaluator.CONTEXT_OBSERVATION_EXPIRED
        ), stale.explanationCodes());
    }

    @Test
    void laterRulesStillRunAfterEarlierIndependentViolations() {
        TripContext base = demoContext();
        TripContext context = replaceTrip(base, trip(NOW, 180));

        HardConstraintPlanEvaluation nearest = evaluation(
                evaluate(context), RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_A
        );

        assertFalse(nearest.feasible());
        assertTrue(nearest.violatedHardConstraints().contains(HardConstraintEvaluator.VEHICLE_COMPATIBILITY_RULE));
        assertTrue(nearest.violatedHardConstraints().contains(HardConstraintEvaluator.ROUTE_DIMENSION_RULE));
        assertTrue(nearest.violatedHardConstraints().contains(HardConstraintEvaluator.DELIVERY_DEADLINE_RULE));
        assertEquals(13, nearest.assessments().size());
    }

    @Test
    void exactHeightWaitDriverAndDeadlineBoundariesPass() {
        ObservationMetadata fresh = observation(NOW.minusSeconds(30), NOW.plusSeconds(600), 0.98);
        ProviderContext workshop = provider(
                WORKSHOP_A,
                "Workshop A - Boundary",
                ProviderType.WORKSHOP,
                ProviderAvailabilityStatus.AVAILABLE,
                List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR"),
                30,
                true,
                fresh
        );
        TripContext base = demoContext();
        VehicleContext vehicle = vehicle(decimal("3.50"), decimal("14000"), true);
        DriverContext driver = driver(30, DriverStatus.DRIVING, fresh);
        CargoContext cargo = cargo(decimal("8"), fresh);
        RouteContext route = route(
                RouteStatus.ACTIVE,
                0,
                List.of(heightRestriction(decimal("3.50"), fresh)),
                fresh
        );
        Trip exactDeadlineAndWait = trip(NOW.plusSeconds(185 * 60L), 30);
        TripContext context = context(
                exactDeadlineAndWait, vehicle, driver, cargo, route,
                List.of(workshop), List.of(), NetworkState.ONLINE
        );

        HardConstraintPlanEvaluation evaluation = evaluation(
                evaluate(context), RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_A
        );

        assertTrue(evaluation.feasible());
        assertEquals(HardConstraintStatus.SATISFIED,
                assessment(evaluation, HardConstraintEvaluator.ROUTE_DIMENSION_RULE).status());
        assertEquals(HardConstraintStatus.SATISFIED,
                assessment(evaluation, HardConstraintEvaluator.WAITING_TIME_RULE).status());
        assertEquals(HardConstraintStatus.SATISFIED,
                assessment(evaluation, HardConstraintEvaluator.DRIVER_HOURS_RULE).status());
        assertEquals(HardConstraintStatus.SATISFIED,
                assessment(evaluation, HardConstraintEvaluator.DELIVERY_DEADLINE_RULE).status());
        assertEquals(decimal("8"), cargo.currentTemperatureCelsius());
        assertNotEquals(base.trip().deliveryDeadline(), context.trip().deliveryDeadline());
    }

    @Test
    void unsafeUndersizedSafeStopReportsAllIndependentViolations() {
        ObservationMetadata fresh = observation(NOW.minusSeconds(30), NOW.plusSeconds(600), 0.98);
        ProviderContext mobile = provider(
                MOBILE_MECHANIC,
                "Mobile Mechanic D",
                ProviderType.MOBILE_MECHANIC,
                ProviderAvailabilityStatus.AVAILABLE,
                List.of("MOBILE_ENGINE_COOLING_REPAIR"),
                30,
                true,
                fresh
        );
        SafeStopContext unsafe = new SafeStopContext(
                SAFE_STOP_A,
                "Unsafe Stop",
                new GeoPoint(11.4, 77.6),
                decimal("3.50"),
                decimal("13000"),
                false,
                fresh
        );
        TripContext base = demoContext();
        TripContext context = context(
                base.trip(), base.vehicle(), base.driver(), base.cargo(),
                route(RouteStatus.ACTIVE, 360, List.of(), fresh),
                List.of(mobile), List.of(unsafe), NetworkState.ONLINE
        );

        HardConstraintPlanEvaluation mobilePlan = evaluation(
                evaluate(context), RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC, MOBILE_MECHANIC
        );

        assertEquals(List.of(
                HardConstraintEvaluator.ROUTE_DIMENSION_RULE,
                HardConstraintEvaluator.SAFE_STOP_RULE
        ), mobilePlan.violatedHardConstraints());
        assertEquals(List.of(
                HardConstraintEvaluator.ROAD_HEIGHT_LIMIT_EXCEEDED,
                HardConstraintEvaluator.ROAD_WEIGHT_LIMIT_EXCEEDED,
                HardConstraintEvaluator.STOP_LOCATION_UNSAFE
        ), mobilePlan.explanationCodes());
    }

    @Test
    void unavailableIncompatibleProviderWithNoResourceReportsEveryCause() {
        ObservationMetadata expired = observation(NOW.minusSeconds(1800), NOW.minusSeconds(1200), 0.95);
        ProviderContext provider = provider(
                WORKSHOP_B,
                "Workshop B - Broken Evidence",
                ProviderType.WORKSHOP,
                ProviderAvailabilityStatus.UNAVAILABLE,
                List.of(),
                45,
                false,
                expired
        );
        TripContext base = demoContext();
        TripContext context = context(
                base.trip(), base.vehicle(), base.driver(), base.cargo(),
                route(RouteStatus.ACTIVE, 360, List.of(), base.route().conditionObservation()),
                List.of(provider), List.of(), NetworkState.ONLINE
        );

        HardConstraintPlanEvaluation workshop = evaluation(
                evaluate(context), RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_B
        );

        assertEquals(List.of(
                HardConstraintEvaluator.VEHICLE_COMPATIBILITY_RULE,
                HardConstraintEvaluator.PROVIDER_FRESHNESS_RULE,
                HardConstraintEvaluator.RESOURCE_RULE,
                HardConstraintEvaluator.CONTEXT_FRESHNESS_RULE
        ), workshop.violatedHardConstraints());
        assertEquals(List.of(
                HardConstraintEvaluator.VEHICLE_TYPE_UNSUPPORTED,
                HardConstraintEvaluator.PROVIDER_UNAVAILABLE,
                HardConstraintEvaluator.PROVIDER_AVAILABILITY_EXPIRED,
                HardConstraintEvaluator.REQUIRED_EQUIPMENT_UNAVAILABLE,
                HardConstraintEvaluator.CONTEXT_OBSERVATION_EXPIRED
        ), workshop.explanationCodes());
    }

    @Test
    void cargoOutsideBoundaryAndMissingRefrigerationRejectIndependently() {
        TripContext base = demoContext();
        ObservationMetadata fresh = base.cargo().conditionObservation();
        TripContext context = context(
                base.trip(),
                vehicle(decimal("3.80"), decimal("14000"), false),
                base.driver(),
                cargo(decimal("8.01"), fresh),
                route(RouteStatus.ACTIVE, 360, List.of(), base.route().conditionObservation()),
                List.of(providerB(fresh)),
                List.of(),
                NetworkState.ONLINE
        );

        HardConstraintPlanEvaluation workshop = evaluation(
                evaluate(context), RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_B
        );

        assertTrue(workshop.violatedHardConstraints().contains(HardConstraintEvaluator.CARGO_SAFETY_RULE));
        assertTrue(workshop.violatedHardConstraints().contains(HardConstraintEvaluator.CARGO_TEMPERATURE_RULE));
        assertTrue(workshop.explanationCodes().contains(HardConstraintEvaluator.CARGO_REQUIREMENT_UNSUPPORTED));
        assertTrue(workshop.explanationCodes().contains(HardConstraintEvaluator.CARGO_TEMPERATURE_AT_RISK));
    }

    @Test
    void blockedRouteAndExpiredRouteEvidenceRejectNormalPlanButNotEmergencyFallback() {
        TripContext base = demoContext();
        ObservationMetadata expired = observation(NOW.minusSeconds(1800), NOW.minusSeconds(1200), 0.95);
        TripContext context = context(
                base.trip(), base.vehicle(), base.driver(), base.cargo(),
                route(RouteStatus.BLOCKED, 360, List.of(), expired),
                List.of(providerB(base.vehicle().healthObservation())),
                List.of(),
                NetworkState.ONLINE
        );

        HardConstraintEvaluationResult result = evaluate(context);
        HardConstraintPlanEvaluation workshop = evaluation(
                result, RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_B
        );
        HardConstraintPlanEvaluation emergency = evaluation(
                result, RecoveryPlanType.EMERGENCY_ESCALATION, null
        );

        assertEquals(List.of(
                HardConstraintEvaluator.ROUTE_BLOCK_RULE,
                HardConstraintEvaluator.CONTEXT_FRESHNESS_RULE
        ), workshop.violatedHardConstraints());
        assertTrue(emergency.feasible());
        assertTrue(emergency.assessments().stream().allMatch(assessment ->
                assessment.status() == HardConstraintStatus.NOT_APPLICABLE));
    }

    @Test
    void lowConfidenceSelectedProviderIsPromotedAndCannotSatisfyHardFacts() {
        ObservationMetadata lowConfidence = observation(NOW.minusSeconds(30), NOW.plusSeconds(600), 0.50);
        TripContext base = demoContext();
        TripContext context = context(
                base.trip(), base.vehicle(), base.driver(), base.cargo(),
                route(RouteStatus.ACTIVE, 360, List.of(), base.route().conditionObservation()),
                List.of(providerB(lowConfidence)),
                List.of(),
                NetworkState.ONLINE
        );

        HardConstraintPlanEvaluation workshop = evaluation(
                evaluate(context), RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_B
        );

        assertFalse(workshop.feasible());
        assertEquals(List.of(HardConstraintEvaluator.LOW_CONTEXT_CONFIDENCE), workshop.warnings());
        assertTrue(workshop.violatedHardConstraints().contains(HardConstraintEvaluator.PROVIDER_FRESHNESS_RULE));
        assertTrue(workshop.violatedHardConstraints().contains(HardConstraintEvaluator.CONTEXT_FRESHNESS_RULE));
        assertEquals(0.50, workshop.confidence());
    }

    @Test
    void providerAgeAtExactMaximumIsStillConfirmed() {
        ObservationMetadata exactAge = observation(NOW.minusSeconds(600), NOW.plusSeconds(60), 0.95);
        TripContext base = demoContext();
        TripContext context = context(
                base.trip(), base.vehicle(), base.driver(), base.cargo(),
                route(RouteStatus.ACTIVE, 360, List.of(), base.route().conditionObservation()),
                List.of(providerB(exactAge)),
                List.of(),
                NetworkState.ONLINE
        );

        HardConstraintPlanEvaluation workshop = evaluation(
                evaluate(context), RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_B
        );

        assertTrue(workshop.feasible());
        assertEquals(HardConstraintStatus.SATISFIED,
                assessment(workshop, HardConstraintEvaluator.PROVIDER_FRESHNESS_RULE).status());
    }

    @Test
    void planInputOrderCannotChangeEvaluationOrderOrHashes() {
        TripContext context = demoContext();
        List<RecoveryPlan> plans = new ArrayList<>(generatedPlans(context));
        HardConstraintEvaluationResult ordered = evaluator.evaluate(context, plans);
        Collections.reverse(plans);

        HardConstraintEvaluationResult reversed = evaluator.evaluate(context, plans);

        assertEquals(ordered, reversed);
        assertTrue(ordered.planEvaluations().stream().allMatch(evaluation ->
                evaluation.inputHash().matches("[a-f0-9]{64}")));
    }

    @Test
    void rejectsPlanFromAnotherTripSnapshot() {
        TripContext context = demoContext();
        RecoveryPlan plan = generatedPlans(context).getFirst();
        RecoveryPlan mismatched = new RecoveryPlan(
                plan.id(),
                UUID.randomUUID(),
                plan.disruptionId(),
                plan.type(),
                plan.status(),
                plan.providerId(),
                plan.safeStopId(),
                plan.steps(),
                plan.evaluation(),
                plan.generatedAt(),
                plan.selectedAt()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                evaluator.evaluate(context, List.of(mismatched)));

        assertEquals("Recovery plan trip does not match the context trip", exception.getMessage());
    }

    @Test
    void widthWeightDriverAndWaitViolationsAreAllReported() {
        TripContext base = demoContext();
        ObservationMetadata fresh = base.route().conditionObservation();
        TripContext context = context(
                trip(NOW.plusSeconds(20 * 3600L), 44),
                base.vehicle(),
                driver(29, DriverStatus.DRIVING, fresh),
                base.cargo(),
                route(
                        RouteStatus.ACTIVE,
                        0,
                        List.of(
                                restriction("GLOBAL_WIDTH", "WIDTH", decimal("2.49"), fresh),
                                restriction("GLOBAL_WEIGHT", "WEIGHT", decimal("13999"), fresh)
                        ),
                        fresh
                ),
                List.of(providerB(fresh)),
                List.of(),
                NetworkState.ONLINE
        );

        HardConstraintPlanEvaluation workshop = evaluation(
                evaluate(context), RecoveryPlanType.WORKSHOP_REPAIR, WORKSHOP_B
        );

        assertEquals(List.of(
                HardConstraintEvaluator.ROUTE_DIMENSION_RULE,
                HardConstraintEvaluator.DRIVER_HOURS_RULE,
                HardConstraintEvaluator.WAITING_TIME_RULE
        ), workshop.violatedHardConstraints());
        assertEquals(List.of(
                HardConstraintEvaluator.ROAD_WEIGHT_LIMIT_EXCEEDED,
                HardConstraintEvaluator.ROAD_WIDTH_LIMIT_EXCEEDED,
                HardConstraintEvaluator.DRIVER_HOURS_INSUFFICIENT,
                HardConstraintEvaluator.WAITING_TIME_EXCEEDED
        ), workshop.explanationCodes());
    }

    @Test
    void missingPlanSpecificProviderCapabilityRejectsMobileCandidate() {
        TripContext base = demoContext();
        ObservationMetadata fresh = base.vehicle().healthObservation();
        ProviderContext mobileWithoutCapability = provider(
                MOBILE_MECHANIC,
                "Mobile Mechanic Without Cooling Repair",
                ProviderType.MOBILE_MECHANIC,
                ProviderAvailabilityStatus.AVAILABLE,
                List.of(),
                30,
                true,
                fresh
        );
        TripContext context = context(
                base.trip(), base.vehicle(), base.driver(), base.cargo(),
                route(RouteStatus.ACTIVE, 360, List.of(), fresh),
                List.of(mobileWithoutCapability),
                List.of(safeStop(SAFE_STOP_A, "Safe Parking North", decimal("5"), decimal("25000"), fresh)),
                NetworkState.ONLINE
        );

        HardConstraintPlanEvaluation mobile = evaluation(
                evaluate(context), RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC, MOBILE_MECHANIC
        );

        assertEquals(List.of(HardConstraintEvaluator.PROVIDER_CAPABILITY_RULE),
                mobile.violatedHardConstraints());
        assertEquals(List.of(HardConstraintEvaluator.PROVIDER_CAPABILITY_MISSING),
                mobile.explanationCodes());
    }

    @Test
    void evaluationHashCoversPlanStepContentNotOnlyPlanId() {
        TripContext context = demoContext();
        RecoveryPlan original = generatedPlans(context).stream()
                .filter(plan -> WORKSHOP_B.equals(plan.providerId()))
                .findFirst()
                .orElseThrow();
        RecoveryPlanStep first = original.steps().getFirst();
        RecoveryPlanStep changedFirst = new RecoveryPlanStep(
                first.sequence(),
                first.actionCode(),
                first.instruction() + " Confirm the handoff.",
                first.providerId(),
                first.expectedStartAt(),
                first.expectedCompleteAt(),
                first.requiredResources()
        );
        List<RecoveryPlanStep> changedSteps = new ArrayList<>(original.steps());
        changedSteps.set(0, changedFirst);
        RecoveryPlan changed = new RecoveryPlan(
                original.id(),
                original.tripId(),
                original.disruptionId(),
                original.type(),
                original.status(),
                original.providerId(),
                original.safeStopId(),
                changedSteps,
                original.evaluation(),
                original.generatedAt(),
                original.selectedAt()
        );

        String originalHash = evaluator.evaluate(context, List.of(original))
                .planEvaluations().getFirst().inputHash();
        String changedHash = evaluator.evaluate(context, List.of(changed))
                .planEvaluations().getFirst().inputHash();

        assertNotEquals(originalHash, changedHash);
    }

    @Test
    void assessmentDefensivelyCopiesListEvidence() {
        List<String> mutableEvidence = new ArrayList<>(List.of("first"));
        HardConstraintAssessment assessment = new HardConstraintAssessment(
                "TEST-001",
                HardConstraintStatus.SATISFIED,
                List.of(),
                Map.of("values", mutableEvidence)
        );

        mutableEvidence.clear();

        assertEquals(List.of("first"), assessment.evidence().get("values"));
    }

    private HardConstraintEvaluationResult evaluate(TripContext context) {
        return evaluator.evaluate(context, generatedPlans(context));
    }

    private List<RecoveryPlan> generatedPlans(TripContext context) {
        return generator.generate(context, DISRUPTION_ID, overheating(context)).plans();
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

    private static HardConstraintPlanEvaluation evaluation(
            HardConstraintEvaluationResult result,
            RecoveryPlanType type,
            UUID providerId
    ) {
        return result.planEvaluations().stream()
                .filter(evaluation -> evaluation.plan().type() == type)
                .filter(evaluation -> providerId == null || providerId.equals(evaluation.plan().providerId()))
                .findFirst()
                .orElseThrow();
    }

    private static HardConstraintAssessment assessment(
            HardConstraintPlanEvaluation evaluation,
            String ruleId
    ) {
        return evaluation.assessments().stream()
                .filter(assessment -> assessment.ruleId().equals(ruleId))
                .findFirst()
                .orElseThrow();
    }

    private static TripContext demoContext() {
        ObservationMetadata fresh = observation(NOW.minusSeconds(30), NOW.plusSeconds(600), 0.98);
        ObservationMetadata stale = observation(NOW.minusSeconds(1800), NOW.minusSeconds(1200), 0.90);
        return context(
                trip(NOW.plusSeconds(20 * 3600L), 180),
                vehicle(decimal("3.80"), decimal("14000"), true),
                driver(360, DriverStatus.DRIVING, fresh),
                cargo(decimal("6"), fresh),
                route(
                        RouteStatus.ACTIVE,
                        360,
                        List.of(heightRestriction(decimal("3.50"), fresh)),
                        fresh
                ),
                List.of(
                        provider(
                                WORKSHOP_C,
                                "Workshop C - Stale Availability",
                                ProviderType.WORKSHOP,
                                ProviderAvailabilityStatus.AVAILABLE,
                                List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR"),
                                35,
                                true,
                                stale
                        ),
                        provider(
                                TOWING_SERVICE,
                                "Towing Service E",
                                ProviderType.TOWING_SERVICE,
                                ProviderAvailabilityStatus.AVAILABLE,
                                List.of("HEAVY_VEHICLE_TOWING"),
                                60,
                                true,
                                fresh
                        ),
                        provider(
                                WORKSHOP_A,
                                "Workshop A - Nearest Incompatible",
                                ProviderType.WORKSHOP,
                                ProviderAvailabilityStatus.AVAILABLE,
                                List.of("HEAVY_VEHICLE_REPAIR"),
                                20,
                                true,
                                fresh
                        ),
                        provider(
                                MOBILE_MECHANIC,
                                "Mobile Mechanic D - Replanning Fallback",
                                ProviderType.MOBILE_MECHANIC,
                                ProviderAvailabilityStatus.AVAILABLE,
                                List.of("MOBILE_ENGINE_COOLING_REPAIR"),
                                55,
                                true,
                                fresh
                        ),
                        providerB(fresh)
                ),
                List.of(
                        safeStop(SAFE_STOP_B, "Safe Parking South", decimal("4.50"), decimal("20000"), fresh),
                        safeStop(SAFE_STOP_A, "Safe Parking North", decimal("5.00"), decimal("25000"), fresh)
                ),
                NetworkState.ONLINE
        );
    }

    private static TripContext context(
            Trip trip,
            VehicleContext vehicle,
            DriverContext driver,
            CargoContext cargo,
            RouteContext route,
            List<ProviderContext> providers,
            List<SafeStopContext> safeStops,
            NetworkState networkState
    ) {
        return new TripContext(
                trip,
                vehicle,
                driver,
                cargo,
                route,
                providers,
                safeStops,
                List.of(),
                networkState,
                List.of(),
                null,
                NOW,
                HASH
        );
    }

    private static TripContext replaceTrip(TripContext context, Trip trip) {
        return context(
                trip,
                context.vehicle(),
                context.driver(),
                context.cargo(),
                context.route(),
                context.providers(),
                context.safeStops(),
                context.networkState()
        );
    }

    private static Trip trip(Instant deadline, int maximumWaitingMinutes) {
        return new Trip(
                TRIP_ID,
                id("00000000-0000-4000-8000-000000000001"),
                id("00000000-0000-4000-8300-000000000001"),
                id("00000000-0000-4000-8200-000000000001"),
                id("00000000-0000-4000-8400-000000000001"),
                id("00000000-0000-4000-8500-000000000001"),
                "TRIP-DEMO-001",
                TripStatus.DISRUPTED,
                deadline,
                maximumWaitingMinutes,
                90,
                NOW.minusSeconds(600),
                NOW.minusSeconds(700),
                NOW.minusSeconds(5)
        );
    }

    private static VehicleContext vehicle(
            BigDecimal heightMeters,
            BigDecimal grossWeightKg,
            boolean refrigerationSupported
    ) {
        return new VehicleContext(
                id("00000000-0000-4000-8300-000000000001"),
                "TN-NEEPO-01",
                "REFRIGERATED_TRUCK",
                VehicleStatus.DISRUPTED,
                heightMeters,
                decimal("2.50"),
                grossWeightKg,
                decimal("7000"),
                decimal("105"),
                decimal("112"),
                decimal("92"),
                refrigerationSupported,
                List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR"),
                observation(NOW.minusSeconds(30), NOW.plusSeconds(600), 0.98)
        );
    }

    private static DriverContext driver(
            int remainingMinutes,
            DriverStatus status,
            ObservationMetadata observation
    ) {
        return new DriverContext(
                id("00000000-0000-4000-8200-000000000001"),
                status,
                remainingMinutes,
                540,
                NOW.minusSeconds(3600),
                observation
        );
    }

    private static CargoContext cargo(BigDecimal currentTemperature, ObservationMetadata observation) {
        return new CargoContext(
                id("00000000-0000-4000-8400-000000000001"),
                CargoType.REFRIGERATED,
                decimal("2500"),
                decimal("2"),
                decimal("8"),
                currentTemperature,
                false,
                List.of("MAINTAIN_TEMPERATURE"),
                observation
        );
    }

    private static RouteContext route(
            RouteStatus status,
            int remainingMinutes,
            List<RouteRestriction> restrictions,
            ObservationMetadata observation
    ) {
        return new RouteContext(
                id("00000000-0000-4000-8500-000000000001"),
                status,
                new GeoPoint(11.3410, 77.7172),
                new GeoPoint(12.9716, 77.5946),
                new GeoPoint(11.4, 77.65),
                decimal("260"),
                remainingMinutes,
                restrictions,
                observation
        );
    }

    private static RouteRestriction heightRestriction(
            BigDecimal limit,
            ObservationMetadata observation
    ) {
        return restriction("WORKSHOP_A_ACCESS_HEIGHT", "HEIGHT", limit, observation);
    }

    private static RouteRestriction restriction(
            String code,
            String kind,
            BigDecimal limit,
            ObservationMetadata observation
    ) {
        return new RouteRestriction(
                UUID.nameUUIDFromBytes(code.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                code,
                kind,
                limit,
                "WEIGHT".equals(kind) ? "KILOGRAMS" : "METERS",
                false,
                observation
        );
    }

    private static ProviderContext providerB(ObservationMetadata observation) {
        return provider(
                WORKSHOP_B,
                "Workshop B - Compatible",
                ProviderType.WORKSHOP,
                ProviderAvailabilityStatus.AVAILABLE,
                List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR"),
                45,
                true,
                observation
        );
    }

    private static ProviderContext provider(
            UUID providerId,
            String name,
            ProviderType type,
            ProviderAvailabilityStatus availability,
            List<String> capabilities,
            Integer arrivalMinutes,
            boolean resourceAvailable,
            ObservationMetadata observation
    ) {
        return new ProviderContext(
                providerId,
                name,
                type,
                new GeoPoint(11.5, 77.7),
                capabilities,
                availability,
                arrivalMinutes,
                decimal("0.90"),
                resourceAvailable,
                observation
        );
    }

    private static SafeStopContext safeStop(
            UUID id,
            String name,
            BigDecimal maximumHeight,
            BigDecimal maximumWeight,
            ObservationMetadata observation
    ) {
        return new SafeStopContext(
                id,
                name,
                new GeoPoint(11.45, 77.68),
                maximumHeight,
                maximumWeight,
                true,
                observation
        );
    }

    private static ObservationMetadata observation(
            Instant observedAt,
            Instant expiresAt,
            double confidence
    ) {
        return new ObservationMetadata(
                observedAt,
                observedAt.plusSeconds(1),
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
