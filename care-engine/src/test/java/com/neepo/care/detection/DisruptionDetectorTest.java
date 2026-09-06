package com.neepo.care.detection;

import com.neepo.care.context.ContextFreshnessPolicy;
import com.neepo.care.context.ContextFreshnessValidator;
import com.neepo.contracts.CargoContext;
import com.neepo.contracts.CargoType;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import com.neepo.contracts.DriverContext;
import com.neepo.contracts.DriverStatus;
import com.neepo.contracts.NetworkState;
import com.neepo.contracts.ObservationMetadata;
import com.neepo.contracts.ObservationStatus;
import com.neepo.contracts.RouteContext;
import com.neepo.contracts.RouteStatus;
import com.neepo.contracts.Trip;
import com.neepo.contracts.TripContext;
import com.neepo.contracts.TripStatus;
import com.neepo.contracts.VehicleContext;
import com.neepo.contracts.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisruptionDetectorTest {
    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");
    private static final String HASH = "b".repeat(64);
    private static final ObservationMetadata FRESH = metadata(
            NOW.minusSeconds(10), NOW.minusSeconds(9), NOW.plusSeconds(300), 0.95,
            ObservationStatus.FRESH
    );

    private final DisruptionDetector detector = detector(DisruptionDetectionPolicy.demoDefaults());

    @Test
    void temperatureAtExactSafetyBoundariesIsClear() {
        DisruptionDetectionResult result = detector.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("105"), decimal("105"), FRESH,
                decimal("8"), decimal("2"), decimal("8"), FRESH,
                FRESH
        ));

        assertTrue(result.disruptions().isEmpty());
        assertEquals(List.of(DetectionRuleStatus.CLEAR, DetectionRuleStatus.CLEAR),
                result.ruleAssessments().stream().map(DisruptionRuleAssessment::status).toList());
    }

    @Test
    void detectsVehicleOverheatingAboveConfiguredVehicleLimit() {
        DisruptionDetectionResult result = detector.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("112"), decimal("105"), FRESH,
                decimal("6"), decimal("2"), decimal("8"), FRESH,
                FRESH
        ));

        assertEquals(1, result.disruptions().size());
        DetectedDisruption disruption = result.disruptions().getFirst();
        assertEquals(DisruptionType.VEHICLE_OVERHEATING, disruption.type());
        assertEquals(DisruptionSeverity.HIGH, disruption.severity());
        assertEquals(NOW, disruption.detectedAt());
        assertEquals(decimal("7"), disruption.evidence().get("excessCelsius"));
        assertEquals(DisruptionDetector.OVERHEATING_RULE, disruption.evidence().get("ruleId"));
        assertTrue(disruption.explanationCodes().isEmpty());
    }

    @Test
    void expiredVehicleEvidenceBlocksOverheatingDetection() {
        ObservationMetadata expiredAtBoundary = metadata(
                NOW.minusSeconds(60), NOW.minusSeconds(59), NOW, 0.99, ObservationStatus.FRESH
        );

        DisruptionDetectionResult result = detector.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("112"), decimal("105"), expiredAtBoundary,
                decimal("6"), decimal("2"), decimal("8"), FRESH,
                FRESH
        ));

        assertTrue(result.disruptions().isEmpty());
        assertEquals(
                DetectionRuleStatus.INSUFFICIENT_CONTEXT,
                assessment(result, DisruptionType.VEHICLE_OVERHEATING).status()
        );
        assertEquals(
                List.of("VEHICLE_HEALTH_OBSERVATION_UNUSABLE"),
                assessment(result, DisruptionType.VEHICLE_OVERHEATING).blockingReasons()
        );
    }

    @Test
    void futureVehicleEvidenceBlocksOverheatingDetection() {
        ObservationMetadata future = metadata(
                NOW.plusSeconds(1), NOW.plusSeconds(2), NOW.plusSeconds(60), 0.99,
                ObservationStatus.FRESH
        );

        DisruptionDetectionResult result = detector.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("112"), decimal("105"), future,
                decimal("6"), decimal("2"), decimal("8"), FRESH,
                FRESH
        ));

        assertTrue(result.disruptions().isEmpty());
        assertEquals(
                DetectionRuleStatus.INSUFFICIENT_CONTEXT,
                assessment(result, DisruptionType.VEHICLE_OVERHEATING).status()
        );
    }

    @Test
    void detectsCargoTemperatureAboveMaximumWithApprovedExplanationCode() {
        DisruptionDetectionResult result = detector.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("95"), decimal("105"), FRESH,
                decimal("10.5"), decimal("2"), decimal("8"), FRESH,
                FRESH
        ));

        DetectedDisruption disruption = result.disruptions().getFirst();
        assertEquals(DisruptionType.CARGO_TEMPERATURE_DEVIATION, disruption.type());
        assertEquals("ABOVE_MAXIMUM", disruption.evidence().get("direction"));
        assertEquals(decimal("2.5"), disruption.evidence().get("deviationCelsius"));
        assertEquals(List.of(DisruptionDetector.CARGO_TEMPERATURE_CODE), disruption.explanationCodes());
    }

    @Test
    void detectsCargoTemperatureBelowMinimum() {
        DisruptionDetectionResult result = detector.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("95"), decimal("105"), FRESH,
                decimal("-1"), decimal("2"), decimal("8"), FRESH,
                FRESH
        ));

        DetectedDisruption disruption = result.disruptions().getFirst();
        assertEquals("BELOW_MINIMUM", disruption.evidence().get("direction"));
        assertEquals(decimal("3"), disruption.evidence().get("deviationCelsius"));
    }

    @Test
    void simultaneousDetectionsUseStableSeverityThenEnumTypeOrder() {
        DisruptionDetectionResult result = detector.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("112"), decimal("105"), FRESH,
                decimal("10"), decimal("2"), decimal("8"), FRESH,
                FRESH
        ));

        assertEquals(List.of(
                        DisruptionType.VEHICLE_OVERHEATING,
                        DisruptionType.CARGO_TEMPERATURE_DEVIATION),
                result.disruptions().stream().map(DetectedDisruption::type).toList());
    }

    @Test
    void higherSeverityDetectionSortsBeforeEarlierEnumType() {
        DisruptionDetector configured = detector(new DisruptionDetectionPolicy(
                DisruptionSeverity.MEDIUM,
                DisruptionSeverity.CRITICAL
        ));

        DisruptionDetectionResult result = configured.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("112"), decimal("105"), FRESH,
                decimal("10"), decimal("2"), decimal("8"), FRESH,
                FRESH
        ));

        assertEquals(List.of(
                        DisruptionType.CARGO_TEMPERATURE_DEVIATION,
                        DisruptionType.VEHICLE_OVERHEATING),
                result.disruptions().stream().map(DetectedDisruption::type).toList());
    }

    @Test
    void unrelatedExpiredRouteEvidenceDoesNotSuppressOverheatingRule() {
        ObservationMetadata expiredRoute = metadata(
                NOW.minusSeconds(60), NOW.minusSeconds(59), NOW, 0.99, ObservationStatus.FRESH
        );

        DisruptionDetectionResult result = detector.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("112"), decimal("105"), FRESH,
                decimal("6"), decimal("2"), decimal("8"), FRESH,
                expiredRoute
        ));

        assertEquals(DisruptionType.VEHICLE_OVERHEATING, result.disruptions().getFirst().type());
        assertFalse(result.freshness().hardFactsUsable());
    }

    @Test
    void inactiveTripDoesNotProduceDisruptions() {
        DisruptionDetectionResult result = detector.detect(context(
                TripStatus.ASSIGNED,
                decimal("112"), decimal("105"), FRESH,
                decimal("10"), decimal("2"), decimal("8"), FRESH,
                FRESH
        ));

        assertTrue(result.disruptions().isEmpty());
        assertTrue(result.ruleAssessments().stream().allMatch(assessment ->
                assessment.status() == DetectionRuleStatus.INSUFFICIENT_CONTEXT
                        && assessment.blockingReasons().equals(List.of("TRIP_NOT_ACTIVE"))));
    }

    @Test
    void missingCargoTemperatureBlocksOnlyTemperatureControlledCargoRule() {
        DisruptionDetectionResult controlled = detector.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("95"), decimal("105"), FRESH,
                null, decimal("2"), decimal("8"), FRESH,
                FRESH
        ));
        DisruptionDetectionResult unregulated = detector.detect(context(
                TripStatus.IN_PROGRESS,
                decimal("95"), decimal("105"), FRESH,
                null, null, null, FRESH,
                FRESH
        ));

        assertEquals(
                DetectionRuleStatus.INSUFFICIENT_CONTEXT,
                assessment(controlled, DisruptionType.CARGO_TEMPERATURE_DEVIATION).status()
        );
        assertEquals(
                DetectionRuleStatus.CLEAR,
                assessment(unregulated, DisruptionType.CARGO_TEMPERATURE_DEVIATION).status()
        );
    }

    private DisruptionDetector detector(DisruptionDetectionPolicy policy) {
        return new DisruptionDetector(
                new ContextFreshnessValidator(ContextFreshnessPolicy.defaults()),
                policy
        );
    }

    private DisruptionRuleAssessment assessment(DisruptionDetectionResult result, DisruptionType type) {
        return result.ruleAssessments().stream()
                .filter(assessment -> assessment.type() == type)
                .findFirst()
                .orElseThrow();
    }

    private TripContext context(
            TripStatus tripStatus,
            BigDecimal engineTemperature,
            BigDecimal maximumEngineTemperature,
            ObservationMetadata vehicleObservation,
            BigDecimal cargoTemperature,
            BigDecimal minimumCargoTemperature,
            BigDecimal maximumCargoTemperature,
            ObservationMetadata cargoObservation,
            ObservationMetadata routeObservation
    ) {
        UUID tripId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        return new TripContext(
                new Trip(
                        tripId, UUID.randomUUID(), vehicleId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        "TRIP-1", tripStatus, NOW.plusSeconds(3600), 60, 30,
                        NOW.minusSeconds(600), NOW.minusSeconds(700), NOW.minusSeconds(5)
                ),
                new VehicleContext(
                        vehicleId, "TN-NEEPO-01", "REFRIGERATED_TRUCK", VehicleStatus.IN_TRANSIT,
                        null, null, null, null, maximumEngineTemperature, engineTemperature,
                        decimal("92"), true, List.of(), vehicleObservation
                ),
                new DriverContext(
                        UUID.randomUUID(), DriverStatus.DRIVING, 300, 540,
                        NOW.minusSeconds(3600), FRESH
                ),
                new CargoContext(
                        UUID.randomUUID(), CargoType.REFRIGERATED, decimal("2500"),
                        minimumCargoTemperature, maximumCargoTemperature, cargoTemperature,
                        false, List.of("MAINTAIN_TEMPERATURE"), cargoObservation
                ),
                new RouteContext(
                        UUID.randomUUID(), RouteStatus.ACTIVE, null, null, null, null,
                        60, List.of(), routeObservation
                ),
                List.of(),
                List.of(),
                List.of(),
                NetworkState.ONLINE,
                List.of(),
                null,
                NOW,
                HASH
        );
    }

    private static ObservationMetadata metadata(
            Instant observedAt,
            Instant receivedAt,
            Instant expiresAt,
            double confidence,
            ObservationStatus status
    ) {
        return new ObservationMetadata(observedAt, receivedAt, expiresAt, confidence, status, "SIMULATOR");
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
