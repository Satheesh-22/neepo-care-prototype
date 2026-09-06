package com.neepo.care.orchestration;

import com.neepo.care.detection.DetectedDisruption;
import com.neepo.care.evaluation.HardConstraintEvaluator;
import com.neepo.care.invalidation.ReplanningTrigger;
import com.neepo.care.scoring.RecoveryPlanScorer;
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
import com.neepo.contracts.RecoveryPlanStep;
import com.neepo.contracts.RecoveryPlanType;
import com.neepo.contracts.RouteContext;
import com.neepo.contracts.RouteStatus;
import com.neepo.contracts.Trip;
import com.neepo.contracts.TripContext;
import com.neepo.contracts.TripStatus;
import com.neepo.contracts.VehicleContext;
import com.neepo.contracts.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryReplannerTest {
    private static final Instant NOW = Instant.parse("2026-08-24T09:00:00Z");
    private static final String HASH = "b".repeat(64);
    private static final UUID TRIP_ID = id("00000000-0000-4000-8600-000000000001");
    private static final UUID DISRUPTION_ID = id("00000000-0000-4000-9000-000000000001");
    private static final UUID INVALIDATED_PLAN_ID = id("00000000-0000-4000-9100-000000000099");
    private static final UUID INVALIDATION_ID = id("00000000-0000-5000-9200-000000000001");
    private static final UUID EVENT_ID = id("10000000-0000-4000-9000-000000000003");
    private static final UUID CANCELLED_PROVIDER = id("00000000-0000-4000-8700-000000000001");
    private static final UUID BACKUP_PROVIDER = id("00000000-0000-4000-8700-000000000002");

    private final RecoveryDecisionEngine decisionEngine = RecoveryDecisionEngine.demoDefaults();
    private final RecoveryReplanner replanner = new RecoveryReplanner(decisionEngine);

    @Test
    void replanningRejectsCancelledProviderAndSelectsAvailableBackup() {
        TripContext context = context(
                invalidatedPlan(),
                List.of(
                        provider(CANCELLED_PROVIDER, ProviderAvailabilityStatus.CANCELLED, 15, "95"),
                        provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")
                ),
                HASH
        );

        RecoveryReplanningResult result = replanner.replan(context, trigger(), activeOverheating());

        assertEquals(BACKUP_PROVIDER, result.decision().selection().selectedPlan().providerId());
        assertEquals(RecoveryPlanStatus.SELECTED, result.decision().selection().selectedPlan().status());
        assertNotEquals(INVALIDATED_PLAN_ID, result.decision().selection().selectedPlan().id());
        assertTrue(result.decision().hardEvaluation().planEvaluations().stream()
                .filter(evaluation -> CANCELLED_PROVIDER.equals(evaluation.plan().providerId()))
                .findFirst()
                .orElseThrow()
                .explanationCodes()
                .contains(HardConstraintEvaluator.PROVIDER_UNAVAILABLE));
    }

    @Test
    void replanningRunsEveryDecisionStageWithoutDroppingCandidates() {
        TripContext context = context(
                invalidatedPlan(),
                List.of(provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")),
                HASH
        );

        RecoveryDecisionResult decision = replanner.replan(
                context, trigger(), activeOverheating()
        ).decision();

        int generated = decision.generation().plans().size();
        assertEquals(generated, decision.hardEvaluation().planEvaluations().size());
        assertEquals(generated, decision.scoring().plans().size());
        assertEquals(generated, decision.selection().plans().size());
        assertEquals(1, decision.selection().plans().stream()
                .filter(plan -> plan.status() == RecoveryPlanStatus.SELECTED)
                .count());
    }

    @Test
    void replayingSameSnapshotAndTriggerProducesSameDecision() {
        TripContext context = context(
                invalidatedPlan(),
                List.of(provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")),
                HASH
        );

        RecoveryReplanningResult first = replanner.replan(context, trigger(), activeOverheating());
        RecoveryReplanningResult replay = replanner.replan(context, trigger(), activeOverheating());

        assertEquals(first, replay);
    }

    @Test
    void activeDisruptionDetectedBeforeNewSnapshotCanBeReplanned() {
        TripContext context = context(
                invalidatedPlan(),
                List.of(provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")),
                HASH
        );
        DetectedDisruption original = new DetectedDisruption(
                DisruptionType.VEHICLE_OVERHEATING,
                DisruptionSeverity.HIGH,
                NOW.minus(Duration.ofHours(1)),
                List.of(),
                Map.of("ruleId", "VEHICLE-OVERHEAT-DETECT-001")
        );

        RecoveryReplanningResult result = replanner.replan(context, trigger(), original);

        assertEquals(BACKUP_PROVIDER, result.decision().selection().selectedPlan().providerId());
    }

    @Test
    void initialPlanningStillRequiresDisruptionEvidenceFromCurrentSnapshot() {
        TripContext initialContext = context(
                null,
                List.of(provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")),
                HASH
        );

        assertThrows(IllegalArgumentException.class, () -> decisionEngine.decide(
                initialContext,
                DISRUPTION_ID,
                activeOverheating()
        ));
    }

    @Test
    void futureDisruptionOrTriggerCannotDriveReplanning() {
        TripContext context = context(
                invalidatedPlan(),
                List.of(provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")),
                HASH
        );
        DetectedDisruption futureDisruption = new DetectedDisruption(
                DisruptionType.VEHICLE_OVERHEATING,
                DisruptionSeverity.HIGH,
                NOW.plusSeconds(1),
                List.of(),
                Map.of()
        );
        assertThrows(IllegalArgumentException.class, () ->
                replanner.replan(context, trigger(), futureDisruption));

        ReplanningTrigger futureTrigger = new ReplanningTrigger(
                TRIP_ID, DISRUPTION_ID, INVALIDATION_ID, EVENT_ID, NOW.plusSeconds(1)
        );
        assertThrows(IllegalArgumentException.class, () ->
                replanner.replan(context, futureTrigger, activeOverheating()));
    }

    @Test
    void triggerMustMatchTripAndDisruption() {
        TripContext context = context(
                invalidatedPlan(),
                List.of(provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")),
                HASH
        );
        ReplanningTrigger wrongTrip = new ReplanningTrigger(
                id("00000000-0000-4000-8600-000000000099"),
                DISRUPTION_ID,
                INVALIDATION_ID,
                EVENT_ID,
                NOW.minusSeconds(1)
        );
        ReplanningTrigger wrongDisruption = new ReplanningTrigger(
                TRIP_ID,
                id("00000000-0000-4000-9000-000000000099"),
                INVALIDATION_ID,
                EVENT_ID,
                NOW.minusSeconds(1)
        );

        assertThrows(IllegalArgumentException.class, () ->
                replanner.replan(context, wrongTrip, activeOverheating()));
        assertThrows(IllegalArgumentException.class, () ->
                replanner.replan(context, wrongDisruption, activeOverheating()));
    }

    @Test
    void contextMustContainTheInvalidatedCurrentPlan() {
        TripContext missing = context(
                null,
                List.of(provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")),
                HASH
        );
        RecoveryPlan stillExecuting = withStatus(invalidatedPlan(), RecoveryPlanStatus.EXECUTING);
        TripContext active = context(
                stillExecuting,
                List.of(provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")),
                HASH
        );

        assertThrows(NullPointerException.class, () ->
                replanner.replan(missing, trigger(), activeOverheating()));
        assertThrows(IllegalArgumentException.class, () ->
                replanner.replan(active, trigger(), activeOverheating()));
    }

    @Test
    void unchangedSnapshotCannotReselectTheSameInvalidatedPlanInstance() {
        TripContext initialContext = context(
                null,
                List.of(provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")),
                HASH
        );
        RecoveryPlan initiallySelected = decisionEngine.decide(
                initialContext, DISRUPTION_ID, currentOverheating()
        ).selection().selectedPlan();
        RecoveryPlan invalidated = withStatus(initiallySelected, RecoveryPlanStatus.INVALIDATED);
        TripContext unchanged = context(
                invalidated,
                List.of(provider(BACKUP_PROVIDER, ProviderAvailabilityStatus.AVAILABLE, 30, "90")),
                HASH
        );

        assertThrows(IllegalStateException.class, () ->
                replanner.replan(unchanged, trigger(), activeOverheating()));
    }

    private static ReplanningTrigger trigger() {
        return new ReplanningTrigger(
                TRIP_ID,
                DISRUPTION_ID,
                INVALIDATION_ID,
                EVENT_ID,
                NOW.minusSeconds(1)
        );
    }

    private static DetectedDisruption activeOverheating() {
        return new DetectedDisruption(
                DisruptionType.VEHICLE_OVERHEATING,
                DisruptionSeverity.HIGH,
                NOW.minus(Duration.ofMinutes(20)),
                List.of(),
                Map.of("ruleId", "VEHICLE-OVERHEAT-DETECT-001")
        );
    }

    private static DetectedDisruption currentOverheating() {
        return new DetectedDisruption(
                DisruptionType.VEHICLE_OVERHEATING,
                DisruptionSeverity.HIGH,
                NOW,
                List.of(),
                Map.of("contextHash", HASH, "ruleId", "VEHICLE-OVERHEAT-DETECT-001")
        );
    }

    private static TripContext context(
            RecoveryPlan currentPlan,
            List<ProviderContext> providers,
            String hash
    ) {
        ObservationMetadata observation = observation();
        UUID vehicleId = id("00000000-0000-4000-8300-000000000001");
        return new TripContext(
                new Trip(
                        TRIP_ID,
                        id("00000000-0000-4000-8000-000000000001"),
                        vehicleId,
                        id("00000000-0000-4000-8200-000000000001"),
                        id("00000000-0000-4000-8400-000000000001"),
                        id("00000000-0000-4000-8500-000000000001"),
                        "TRIP-REPLAN-001",
                        TripStatus.DISRUPTED,
                        NOW.plus(Duration.ofHours(10)),
                        90,
                        75,
                        NOW.minus(Duration.ofHours(2)),
                        NOW.minus(Duration.ofHours(3)),
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
                        observation
                ),
                new DriverContext(
                        id("00000000-0000-4000-8200-000000000001"),
                        DriverStatus.DRIVING,
                        360,
                        540,
                        NOW.minus(Duration.ofHours(1)),
                        observation
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
                        observation
                ),
                new RouteContext(
                        id("00000000-0000-4000-8500-000000000001"),
                        RouteStatus.ACTIVE,
                        new GeoPoint(11.3410, 77.7172),
                        new GeoPoint(12.9716, 77.5946),
                        new GeoPoint(11.4, 77.65),
                        decimal("260"),
                        60,
                        List.of(),
                        observation
                ),
                providers,
                List.of(),
                List.of(),
                NetworkState.ONLINE,
                List.of(),
                currentPlan,
                NOW,
                hash
        );
    }

    private static ProviderContext provider(
            UUID providerId,
            ProviderAvailabilityStatus availability,
            int etaMinutes,
            String reliability
    ) {
        return new ProviderContext(
                providerId,
                availability == ProviderAvailabilityStatus.AVAILABLE
                        ? "Backup Workshop"
                        : "Cancelled Workshop",
                ProviderType.WORKSHOP,
                new GeoPoint(11.5, 77.7),
                List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR"),
                availability,
                etaMinutes,
                decimal(reliability),
                true,
                observation()
        );
    }

    private static ObservationMetadata observation() {
        return new ObservationMetadata(
                NOW.minusSeconds(30),
                NOW.minusSeconds(29),
                NOW.plus(Duration.ofHours(2)),
                0.95,
                ObservationStatus.FRESH,
                "SIMULATOR"
        );
    }

    private static RecoveryPlan invalidatedPlan() {
        return new RecoveryPlan(
                INVALIDATED_PLAN_ID,
                TRIP_ID,
                DISRUPTION_ID,
                RecoveryPlanType.WORKSHOP_REPAIR,
                RecoveryPlanStatus.INVALIDATED,
                CANCELLED_PROVIDER,
                null,
                List.of(new RecoveryPlanStep(
                        1,
                        "TRAVEL_TO_WORKSHOP",
                        "Travel to workshop",
                        CANCELLED_PROVIDER,
                        NOW.minus(Duration.ofMinutes(10)),
                        NOW.plus(Duration.ofMinutes(30)),
                        List.of()
                )),
                evaluation(),
                NOW.minus(Duration.ofMinutes(30)),
                NOW.minus(Duration.ofMinutes(15))
        );
    }

    private static PlanEvaluation evaluation() {
        return new PlanEvaluation(
                INVALIDATED_PLAN_ID,
                true,
                List.of(),
                List.of(),
                Map.of("TEST_SCORE", decimal("80.0000")),
                decimal("10.0000"),
                decimal("5.0000"),
                decimal("80.0000"),
                decimal("0.3500"),
                decimal("0.2000"),
                decimal("75.5000"),
                0.95,
                Map.of("vehicle.health", "FRESH"),
                List.of("PLAN_SELECTED_HIGHEST_UTILITY"),
                NOW.minus(Duration.ofMinutes(15)),
                RecoveryPlanScorer.EVALUATOR_VERSION,
                "a".repeat(64)
        );
    }

    private static RecoveryPlan withStatus(RecoveryPlan plan, RecoveryPlanStatus status) {
        return new RecoveryPlan(
                plan.id(), plan.tripId(), plan.disruptionId(), plan.type(), status, plan.providerId(),
                plan.safeStopId(), plan.steps(), plan.evaluation(), plan.generatedAt(), plan.selectedAt()
        );
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static UUID id(String value) {
        return UUID.fromString(value);
    }
}
