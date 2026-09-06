package com.neepo.care.context;

import com.neepo.contracts.CargoContext;
import com.neepo.contracts.CargoType;
import com.neepo.contracts.DriverContext;
import com.neepo.contracts.DriverStatus;
import com.neepo.contracts.NetworkState;
import com.neepo.contracts.ObservationMetadata;
import com.neepo.contracts.ObservationStatus;
import com.neepo.contracts.RouteContext;
import com.neepo.contracts.RouteStatus;
import com.neepo.contracts.TelemetryObservation;
import com.neepo.contracts.TelemetrySource;
import com.neepo.contracts.Trip;
import com.neepo.contracts.TripContext;
import com.neepo.contracts.TripStatus;
import com.neepo.contracts.VehicleContext;
import com.neepo.contracts.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextFreshnessValidatorTest {
    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");
    private static final String HASH = "a".repeat(64);
    private static final ContextFreshnessValidator VALIDATOR = new ContextFreshnessValidator(
            ContextFreshnessPolicy.defaults()
    );

    @Test
    void freshHardFactIsUsableBeyondAgingWindow() {
        ContextFreshnessResult result = validate(hard("vehicle.health", metadata(
                NOW.minusSeconds(10), NOW.minusSeconds(9), NOW.plusSeconds(121), 0.95,
                ObservationStatus.FRESH
        )));

        assertTrue(result.hardFactsUsable());
        assertEquals(ObservationStatus.FRESH, result.assessments().getFirst().status());
        assertEquals(0.95, result.minimumConfidence());
    }

    @Test
    void exactAgingBoundaryIsClassifiedAsAgingAndRemainsUsable() {
        ContextFreshnessResult result = validate(hard("route.condition", metadata(
                NOW.minusSeconds(10), NOW.minusSeconds(9), NOW.plusSeconds(120), 0.90,
                ObservationStatus.FRESH
        )));

        assertTrue(result.hardFactsUsable());
        assertEquals(ObservationStatus.AGING, result.assessments().getFirst().status());
    }

    @Test
    void observationIsExpiredAtItsExactExpiryBoundary() {
        ContextFreshnessResult result = validate(hard("cargo.condition", metadata(
                NOW.minusSeconds(60), NOW.minusSeconds(59), NOW, 0.99,
                ObservationStatus.FRESH
        )));

        assertFalse(result.hardFactsUsable());
        assertTrue(result.hasExpiredRequiredData());
        assertEquals(ObservationStatus.EXPIRED, result.assessments().getFirst().status());
        assertEquals(List.of(ContextFreshnessValidator.EXPIRED_CONTEXT_CODE), result.explanationCodes());
    }

    @Test
    void futureObservationIsUnverifiedAndCannotBecomeAHardFact() {
        ContextFreshnessResult result = validate(hard("driver.status", metadata(
                NOW.plusSeconds(1), NOW.plusSeconds(2), NOW.plusSeconds(60), 0.99,
                ObservationStatus.FRESH
        )));

        assertFalse(result.hardFactsUsable());
        assertTrue(result.hasUnverifiedRequiredData());
        assertEquals(ObservationStatus.UNVERIFIED, result.assessments().getFirst().status());
        assertEquals(List.of(FreshnessIssue.FUTURE_OBSERVATION), result.assessments().getFirst().issues());
    }

    @Test
    void invalidTimestampOrderIsUnverified() {
        ContextFreshnessResult result = validate(hard("vehicle.health", metadata(
                NOW.minusSeconds(10), NOW.minusSeconds(11), NOW.plusSeconds(60), 0.90,
                ObservationStatus.FRESH
        )));

        assertFalse(result.hardFactsUsable());
        assertEquals(
                List.of(FreshnessIssue.INVALID_TEMPORAL_ORDER),
                result.assessments().getFirst().issues()
        );
    }

    @Test
    void hardFactBelowConfidenceThresholdIsUnverified() {
        ContextFreshnessResult result = validate(hard("vehicle.health", metadata(
                NOW.minusSeconds(10), NOW.minusSeconds(9), NOW.plusSeconds(300), 0.79,
                ObservationStatus.FRESH
        )));

        assertFalse(result.hardFactsUsable());
        assertEquals(ObservationStatus.UNVERIFIED, result.assessments().getFirst().status());
        assertEquals(List.of(ContextFreshnessValidator.LOW_CONFIDENCE_CODE), result.explanationCodes());
    }

    @Test
    void exactConfidenceThresholdIsConfirmed() {
        ContextFreshnessResult result = validate(hard("vehicle.health", metadata(
                NOW.minusSeconds(10), NOW.minusSeconds(9), NOW.plusSeconds(300), 0.80,
                ObservationStatus.FRESH
        )));

        assertTrue(result.hardFactsUsable());
        assertTrue(result.explanationCodes().isEmpty());
    }

    @Test
    void lowConfidenceAdvisoryEvidenceWarnsWithoutBlockingHardFacts() {
        ContextFreshnessResult result = validate(advisory("provider.availability", metadata(
                NOW.minusSeconds(10), NOW.minusSeconds(9), NOW.plusSeconds(300), 0.60,
                ObservationStatus.FRESH
        )));

        assertTrue(result.hardFactsUsable());
        assertTrue(result.assessments().getFirst().usableForDecision());
        assertEquals(ObservationStatus.FRESH, result.assessments().getFirst().status());
        assertEquals(List.of(ContextFreshnessValidator.LOW_CONFIDENCE_CODE), result.explanationCodes());
    }

    @Test
    void sourceMarkedUnverifiedRemainsUnverifiedEvenBeforeExpiry() {
        ContextFreshnessResult result = validate(hard("route.condition", metadata(
                NOW.minusSeconds(10), NOW.minusSeconds(9), NOW.plusSeconds(300), 0.95,
                ObservationStatus.UNVERIFIED
        )));

        assertFalse(result.hardFactsUsable());
        assertEquals(List.of(FreshnessIssue.SOURCE_UNVERIFIED), result.assessments().getFirst().issues());
    }

    @Test
    void stateUsesStableKeyOrderAndRejectsDuplicateKeys() {
        DecisionState state = new DecisionState(
                UUID.randomUUID(),
                NOW,
                HASH,
                List.of(
                        advisory("z.provider", metadata(NOW.minusSeconds(2), NOW.minusSeconds(1),
                                NOW.plusSeconds(300), 0.9, ObservationStatus.FRESH)),
                        hard("a.vehicle", metadata(NOW.minusSeconds(2), NOW.minusSeconds(1),
                                NOW.plusSeconds(300), 0.9, ObservationStatus.FRESH))
                )
        );

        assertEquals(List.of("a.vehicle", "z.provider"),
                state.observations().stream().map(DecisionObservation::key).toList());
        assertEquals(List.of("a.vehicle", "z.provider"),
                VALIDATOR.validate(state).freshnessSummary().keySet().stream().toList());

        assertThrows(IllegalArgumentException.class, () -> new DecisionState(
                UUID.randomUUID(), NOW, HASH, List.of(hard("same", null), advisory("same", null))
        ));
    }

    @Test
    void sharedTripContextProjectionUsesOnlyLatestTelemetryObservation() {
        UUID tripId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        ObservationMetadata core = metadata(
                NOW.minusSeconds(10), NOW.minusSeconds(9), NOW.plusSeconds(300), 0.95,
                ObservationStatus.FRESH
        );
        TelemetryObservation older = telemetry(tripId, vehicleId, NOW.minusSeconds(60), 0.99);
        TelemetryObservation latest = telemetry(tripId, vehicleId, NOW.minusSeconds(5), 0.91);
        TripContext context = new TripContext(
                new Trip(
                        tripId, UUID.randomUUID(), vehicleId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        "TRIP-1", TripStatus.IN_PROGRESS, NOW.plusSeconds(3600), 60, 30,
                        NOW.minusSeconds(600), NOW.minusSeconds(700), NOW.minusSeconds(5)
                ),
                new VehicleContext(
                        vehicleId, "KA01AB1234", "REFRIGERATED_TRUCK", VehicleStatus.IN_TRANSIT,
                        null, null, null, null, null, null, null, true, List.of(), core
                ),
                new DriverContext(
                        UUID.randomUUID(), DriverStatus.DRIVING, 300, 540, NOW.minusSeconds(3600), core
                ),
                new CargoContext(
                        UUID.randomUUID(), CargoType.REFRIGERATED, null, null, null, null,
                        false, List.of(), core
                ),
                new RouteContext(
                        UUID.randomUUID(), RouteStatus.ACTIVE, null, null, null, null, 60, List.of(), core
                ),
                List.of(),
                List.of(),
                List.of(older, latest),
                NetworkState.ONLINE,
                List.of(),
                null,
                NOW,
                HASH
        );

        DecisionState state = DecisionState.from(context);

        assertEquals(List.of(
                        "cargo.condition", "driver.status", "route.condition", "telemetry.latest", "vehicle.health"),
                state.observations().stream().map(DecisionObservation::key).toList());
        assertEquals(
                latest.recordedAt(),
                state.observations().stream()
                        .filter(observation -> observation.key().equals("telemetry.latest"))
                        .findFirst().orElseThrow().metadata().observedAt()
        );
    }

    @Test
    void invalidPolicyConfigurationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContextFreshnessPolicy(Duration.ofSeconds(-1), 0.8));
        assertThrows(IllegalArgumentException.class,
                () -> new ContextFreshnessPolicy(Duration.ZERO, 1.01));
    }

    private ContextFreshnessResult validate(DecisionObservation... observations) {
        return VALIDATOR.validate(new DecisionState(
                UUID.randomUUID(), NOW, HASH, List.of(observations)
        ));
    }

    private DecisionObservation hard(String key, ObservationMetadata metadata) {
        return new DecisionObservation(key, ObservationRequirement.HARD_FACT, metadata);
    }

    private DecisionObservation advisory(String key, ObservationMetadata metadata) {
        return new DecisionObservation(key, ObservationRequirement.ADVISORY, metadata);
    }

    private ObservationMetadata metadata(
            Instant observedAt,
            Instant receivedAt,
            Instant expiresAt,
            double confidence,
            ObservationStatus status
    ) {
        return new ObservationMetadata(observedAt, receivedAt, expiresAt, confidence, status, "SIMULATOR");
    }

    private TelemetryObservation telemetry(UUID tripId, UUID vehicleId, Instant recordedAt, double confidence) {
        return new TelemetryObservation(
                UUID.randomUUID(), tripId, vehicleId, recordedAt, recordedAt.plusSeconds(1),
                recordedAt.plusSeconds(300), null, null, null, null, confidence,
                ObservationStatus.FRESH, TelemetrySource.SIMULATOR, NetworkState.ONLINE
        );
    }
}
