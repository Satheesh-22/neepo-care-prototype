package com.neepo.care.telemetry;

import com.neepo.care.context.ContextFreshnessPolicy;
import com.neepo.care.context.ContextFreshnessValidator;
import com.neepo.contracts.ActiveDisruption;
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
import com.neepo.contracts.RecoveryPlanStatus;
import com.neepo.contracts.RecoveryPlanType;
import com.neepo.contracts.RouteContext;
import com.neepo.contracts.RouteStatus;
import com.neepo.contracts.TelemetryPolicyState;
import com.neepo.contracts.Trip;
import com.neepo.contracts.TripContext;
import com.neepo.contracts.TripStatus;
import com.neepo.contracts.VehicleContext;
import com.neepo.contracts.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdaptiveTelemetryPolicyEvaluatorTest {
    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");
    private static final UUID TRIP_ID = id("00000000-0000-4000-8600-000000000001");
    private static final UUID EVENT_ID = id("10000000-0000-4000-9300-000000000001");

    private final AdaptiveTelemetryPolicy policy = AdaptiveTelemetryPolicy.demoDefaults();
    private final AdaptiveTelemetryPolicyEvaluator evaluator = new AdaptiveTelemetryPolicyEvaluator(
            new ContextFreshnessValidator(ContextFreshnessPolicy.defaults()),
            policy
    );

    @Test
    void demoPolicyUsesFrozenIntervals() {
        assertEquals(60, policy.normalIntervalSeconds());
        assertEquals(15, policy.nearDecisionIntervalSeconds());
        assertEquals(5, policy.routeDeviationIntervalSeconds());
        assertEquals(3, policy.emergencyIntervalSeconds());
        assertEquals(120, policy.safelyParkedIntervalSeconds());
    }

    @Test
    void normalFreshOnlineTravelUsesSixtySeconds() {
        TelemetryPolicyDecision decision = evaluator.evaluate(
                context(NetworkState.ONLINE, List.of(), null, NOW.plusSeconds(600)),
                signal(false, false)
        );

        assertEquals(TRIP_ID, decision.tripId());
        assertEquals(EVENT_ID, decision.eventId());
        assertEquals(TelemetryPolicyState.NORMAL_TRAVEL, decision.policyState());
        assertEquals(60, decision.intervalSeconds());
        assertEquals(NOW, decision.decidedAt());
        assertEquals("NORMAL_TRAVEL", decision.explanationCode());
    }

    @Test
    void confirmedNearDecisionPointUsesFifteenSeconds() {
        TelemetryPolicyDecision decision = evaluator.evaluate(
                context(NetworkState.ONLINE, List.of(), null, NOW.plusSeconds(600)),
                signal(true, false)
        );

        assertEquals(TelemetryPolicyState.NEAR_DECISION_POINT, decision.policyState());
        assertEquals(15, decision.intervalSeconds());
    }

    @Test
    void agingOrUnusableRequiredEvidenceAcceleratesMonitoring() {
        TelemetryPolicyDecision aging = evaluator.evaluate(
                context(NetworkState.ONLINE, List.of(), null, NOW.plusSeconds(60)),
                signal(false, false)
        );
        TelemetryPolicyDecision expired = evaluator.evaluate(
                context(NetworkState.ONLINE, List.of(), null, NOW),
                signal(false, false)
        );

        assertEquals(TelemetryPolicyState.NEAR_DECISION_POINT, aging.policyState());
        assertEquals(15, aging.intervalSeconds());
        assertEquals(TelemetryPolicyState.NEAR_DECISION_POINT, expired.policyState());
        assertEquals(15, expired.intervalSeconds());
    }

    @Test
    void agingUnselectedProviderDoesNotAccelerateCoreTripMonitoring() {
        TripContext base = context(NetworkState.ONLINE, List.of(), null, NOW.plusSeconds(600));
        ProviderContext agingProvider = new ProviderContext(
                id("00000000-0000-4000-8700-000000000001"),
                "Aging Unselected Workshop",
                ProviderType.WORKSHOP,
                new GeoPoint(11.5, 77.7),
                List.of(),
                ProviderAvailabilityStatus.AVAILABLE,
                30,
                decimal("90"),
                true,
                new ObservationMetadata(
                        NOW.minusSeconds(30),
                        NOW.minusSeconds(29),
                        NOW.plusSeconds(60),
                        0.95,
                        ObservationStatus.FRESH,
                        "SIMULATOR"
                )
        );
        TripContext withAdvisory = new TripContext(
                base.trip(), base.vehicle(), base.driver(), base.cargo(), base.route(),
                List.of(agingProvider), base.safeStops(), base.telemetryObservations(),
                base.networkState(), base.activeDisruptions(), base.currentRecoveryPlan(),
                base.evaluationTime(), base.contextHash()
        );

        TelemetryPolicyDecision decision = evaluator.evaluate(withAdvisory, signal(false, false));

        assertEquals(TelemetryPolicyState.NORMAL_TRAVEL, decision.policyState());
        assertEquals(60, decision.intervalSeconds());
    }

    @Test
    void invalidatedPlanIsATelemetryDecisionPoint() {
        RecoveryPlan invalidated = plan(
                RecoveryPlanType.WORKSHOP_REPAIR,
                RecoveryPlanStatus.INVALIDATED
        );

        TelemetryPolicyDecision decision = evaluator.evaluate(
                context(NetworkState.ONLINE, List.of(), invalidated, NOW.plusSeconds(600)),
                signal(false, false)
        );

        assertEquals(TelemetryPolicyState.NEAR_DECISION_POINT, decision.policyState());
        assertEquals(15, decision.intervalSeconds());
    }

    @Test
    void routeDeviationUsesFiveSeconds() {
        TelemetryPolicyDecision decision = evaluator.evaluate(
                context(
                        NetworkState.ONLINE,
                        List.of(disruption(DisruptionType.ROUTE_DEVIATION, DisruptionSeverity.HIGH, 1)),
                        null,
                        NOW.plusSeconds(600)
                ),
                signal(false, false)
        );

        assertEquals(TelemetryPolicyState.ROUTE_DEVIATION, decision.policyState());
        assertEquals(5, decision.intervalSeconds());
    }

    @Test
    void highSeverityNonRouteDisruptionUsesEmergencyInterval() {
        TelemetryPolicyDecision decision = evaluator.evaluate(
                context(
                        NetworkState.ONLINE,
                        List.of(disruption(DisruptionType.VEHICLE_OVERHEATING, DisruptionSeverity.HIGH, 1)),
                        null,
                        NOW.plusSeconds(600)
                ),
                signal(false, false)
        );

        assertEquals(TelemetryPolicyState.BREAKDOWN_OR_EMERGENCY, decision.policyState());
        assertEquals(3, decision.intervalSeconds());
    }

    @Test
    void emergencyOutranksSimultaneousRouteDeviationRegardlessOfInputOrder() {
        List<ActiveDisruption> disruptions = new ArrayList<>(List.of(
                disruption(DisruptionType.ROUTE_DEVIATION, DisruptionSeverity.HIGH, 1),
                disruption(DisruptionType.CARGO_TEMPERATURE_DEVIATION, DisruptionSeverity.CRITICAL, 2)
        ));
        TelemetryPolicyDecision forward = evaluator.evaluate(
                context(NetworkState.ONLINE, disruptions, null, NOW.plusSeconds(600)),
                signal(false, false)
        );
        Collections.reverse(disruptions);

        TelemetryPolicyDecision reversed = evaluator.evaluate(
                context(NetworkState.ONLINE, disruptions, null, NOW.plusSeconds(600)),
                signal(false, false)
        );

        assertEquals(forward, reversed);
        assertEquals(TelemetryPolicyState.BREAKDOWN_OR_EMERGENCY, forward.policyState());
    }

    @Test
    void verifiedSafeParkingSlowsMonitoringAfterEmergency() {
        TelemetryPolicyDecision decision = evaluator.evaluate(
                context(
                        NetworkState.ONLINE,
                        List.of(disruption(DisruptionType.VEHICLE_OVERHEATING, DisruptionSeverity.HIGH, 1)),
                        null,
                        NOW.plusSeconds(600)
                ),
                signal(false, true)
        );

        assertEquals(TelemetryPolicyState.SAFELY_PARKED, decision.policyState());
        assertEquals(120, decision.intervalSeconds());
    }

    @Test
    void offlineBufferingOutranksEveryOnlineStateAndHasNoUploadInterval() {
        TelemetryPolicyDecision decision = evaluator.evaluate(
                context(
                        NetworkState.OFFLINE,
                        List.of(disruption(DisruptionType.VEHICLE_BREAKDOWN, DisruptionSeverity.CRITICAL, 1)),
                        plan(RecoveryPlanType.EMERGENCY_ESCALATION, RecoveryPlanStatus.EXECUTING),
                        NOW.plusSeconds(600)
                ),
                signal(true, true)
        );

        assertEquals(TelemetryPolicyState.OFFLINE_BUFFERING, decision.policyState());
        assertNull(decision.intervalSeconds());
    }

    @Test
    void selectedEmergencyPlanUsesEmergencyIntervalWithoutActiveDisruption() {
        RecoveryPlan emergency = plan(
                RecoveryPlanType.EMERGENCY_ESCALATION,
                RecoveryPlanStatus.SELECTED
        );

        TelemetryPolicyDecision decision = evaluator.evaluate(
                context(NetworkState.ONLINE, List.of(), emergency, NOW.plusSeconds(600)),
                signal(false, false)
        );

        assertEquals(TelemetryPolicyState.BREAKDOWN_OR_EMERGENCY, decision.policyState());
        assertEquals(3, decision.intervalSeconds());
    }

    @Test
    void futureSignalOrDisruptionCannotInfluenceCurrentPolicy() {
        TripContext normal = context(NetworkState.ONLINE, List.of(), null, NOW.plusSeconds(600));
        TelemetryPolicySignal futureSignal = new TelemetryPolicySignal(
                EVENT_ID, NOW.plusSeconds(1), false, false
        );
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(normal, futureSignal));

        ActiveDisruption futureDisruption = new ActiveDisruption(
                id("00000000-0000-4000-9000-000000000099"),
                id("10000000-0000-4000-9000-000000000099"),
                DisruptionType.VEHICLE_BREAKDOWN,
                DisruptionSeverity.HIGH,
                NOW.plusSeconds(1),
                List.of(),
                Map.of()
        );
        TripContext future = context(
                NetworkState.ONLINE, List.of(futureDisruption), null, NOW.plusSeconds(600)
        );
        assertThrows(IllegalArgumentException.class, () ->
                evaluator.evaluate(future, signal(false, false)));
    }

    @Test
    void decisionContractEnforcesOfflineAndOnlineIntervalShape() {
        assertThrows(IllegalArgumentException.class, () -> new TelemetryPolicyDecision(
                TRIP_ID,
                EVENT_ID,
                TelemetryPolicyState.OFFLINE_BUFFERING,
                60,
                NOW,
                "OFFLINE_BUFFERING"
        ));
        assertThrows(IllegalArgumentException.class, () -> new TelemetryPolicyDecision(
                TRIP_ID,
                EVENT_ID,
                TelemetryPolicyState.NORMAL_TRAVEL,
                null,
                NOW,
                "NORMAL_TRAVEL"
        ));
    }

    private static TelemetryPolicySignal signal(boolean nearDecision, boolean safelyParked) {
        return new TelemetryPolicySignal(EVENT_ID, NOW, nearDecision, safelyParked);
    }

    private static ActiveDisruption disruption(
            DisruptionType type,
            DisruptionSeverity severity,
            int suffix
    ) {
        return new ActiveDisruption(
                id("00000000-0000-4000-9000-00000000000" + suffix),
                id("10000000-0000-4000-9000-00000000000" + suffix),
                type,
                severity,
                NOW.minusSeconds(30),
                List.of(),
                Map.of("source", "TEST")
        );
    }

    private static TripContext context(
            NetworkState networkState,
            List<ActiveDisruption> disruptions,
            RecoveryPlan currentPlan,
            Instant expiresAt
    ) {
        ObservationMetadata observation = new ObservationMetadata(
                NOW.minusSeconds(30),
                NOW.minusSeconds(29),
                expiresAt,
                0.95,
                ObservationStatus.FRESH,
                "SIMULATOR"
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
                        "TRIP-TELEMETRY-001",
                        TripStatus.IN_PROGRESS,
                        NOW.plus(Duration.ofHours(8)),
                        90,
                        75,
                        NOW.minus(Duration.ofHours(1)),
                        NOW.minus(Duration.ofHours(2)),
                        NOW.minusSeconds(5)
                ),
                new VehicleContext(
                        vehicleId,
                        "TN-NEEPO-01",
                        "REFRIGERATED_TRUCK",
                        VehicleStatus.IN_TRANSIT,
                        decimal("3.80"),
                        decimal("2.50"),
                        decimal("14000"),
                        decimal("7000"),
                        decimal("105"),
                        decimal("95"),
                        decimal("92"),
                        true,
                        List.of(),
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
                        180,
                        List.of(),
                        observation
                ),
                List.of(),
                List.of(),
                List.of(),
                networkState,
                disruptions,
                currentPlan,
                NOW,
                "c".repeat(64)
        );
    }

    private static RecoveryPlan plan(RecoveryPlanType type, RecoveryPlanStatus status) {
        return new RecoveryPlan(
                id("00000000-0000-4000-9100-000000000001"),
                TRIP_ID,
                id("00000000-0000-4000-9000-000000000001"),
                type,
                status,
                null,
                null,
                List.of(),
                null,
                NOW.minus(Duration.ofMinutes(10)),
                NOW.minus(Duration.ofMinutes(5))
        );
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static UUID id(String value) {
        return UUID.fromString(value);
    }
}
