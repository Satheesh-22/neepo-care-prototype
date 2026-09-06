package com.neepo.care.planning;

import com.neepo.care.detection.DetectedDisruption;
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
import com.neepo.contracts.RecoveryPlanStep;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecoveryPlanGeneratorTest {
    private static final Instant NOW = Instant.parse("2026-08-17T12:00:00Z");
    private static final String HASH = "c".repeat(64);
    private static final UUID TRIP_ID = id("00000000-0000-4000-8100-000000000001");
    private static final UUID DISRUPTION_ID = id("00000000-0000-4000-8200-000000000001");
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

    @Test
    void generatesAtLeastThreeOrderedOverheatingCandidatesFromDemoResources() {
        RecoveryPlanGenerationResult result = generator.generate(context(providers(), safeStops()), DISRUPTION_ID, overheating());

        assertEquals(7, result.plans().size());
        assertEquals(6, result.normalPlanCount());
        assertEquals(List.of(
                        RecoveryPlanType.WORKSHOP_REPAIR,
                        RecoveryPlanType.WORKSHOP_REPAIR,
                        RecoveryPlanType.WORKSHOP_REPAIR,
                        RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC,
                        RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC,
                        RecoveryPlanType.TOW_REPAIR_OR_TRANSFER,
                        RecoveryPlanType.EMERGENCY_ESCALATION
                ),
                result.plans().stream().map(RecoveryPlan::type).toList());
        assertEquals(List.of(WORKSHOP_A, WORKSHOP_B, WORKSHOP_C),
                result.plans().stream()
                        .filter(plan -> plan.type() == RecoveryPlanType.WORKSHOP_REPAIR)
                        .map(RecoveryPlan::providerId)
                        .toList());
        assertEquals(List.of(SAFE_STOP_A, SAFE_STOP_B),
                result.plans().stream()
                        .filter(plan -> plan.type() == RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC)
                        .map(RecoveryPlan::safeStopId)
                        .toList());
    }

    @Test
    void repeatedGenerationProducesIdenticalIdsAndTimelines() {
        TripContext context = context(providers(), safeStops());

        RecoveryPlanGenerationResult first = generator.generate(context, DISRUPTION_ID, overheating());
        RecoveryPlanGenerationResult second = generator.generate(context, DISRUPTION_ID, overheating());

        assertEquals(first, second);
        assertTrue(first.plans().stream().allMatch(plan -> plan.id().version() == 5));
        assertTrue(first.plans().stream().allMatch(plan -> plan.generatedAt().equals(NOW)));
    }

    @Test
    void resourceInputOrderCannotChangeCandidateOrderOrIdentity() {
        List<ProviderContext> reversedProviders = new ArrayList<>(providers());
        List<SafeStopContext> reversedStops = new ArrayList<>(safeStops());
        Collections.reverse(reversedProviders);
        Collections.reverse(reversedStops);

        RecoveryPlanGenerationResult ordered = generator.generate(
                context(providers(), safeStops()), DISRUPTION_ID, overheating()
        );
        RecoveryPlanGenerationResult reversed = generator.generate(
                context(reversedProviders, reversedStops), DISRUPTION_ID, overheating()
        );

        assertEquals(ordered, reversed);
    }

    @Test
    void generationPreservesIncompatibleUnavailableAndStaleOptionsForEvaluation() {
        List<RecoveryPlan> workshops = generator.generate(
                        context(providers(), safeStops()), DISRUPTION_ID, overheating()
                ).plans().stream()
                .filter(plan -> plan.type() == RecoveryPlanType.WORKSHOP_REPAIR)
                .toList();

        assertEquals(List.of(WORKSHOP_A, WORKSHOP_B, WORKSHOP_C),
                workshops.stream().map(RecoveryPlan::providerId).toList());
        assertTrue(workshops.stream().allMatch(plan -> plan.status() == RecoveryPlanStatus.GENERATED));
        assertTrue(workshops.stream().allMatch(plan -> plan.evaluation() == null));
        assertTrue(workshops.stream().allMatch(plan -> plan.selectedAt() == null));
    }

    @Test
    void everyTimelineIsSequentialContiguousAndPositive() {
        List<RecoveryPlan> plans = generator.generate(
                context(providers(), safeStops()), DISRUPTION_ID, overheating()
        ).plans();

        for (RecoveryPlan plan : plans) {
            assertEquals(TRIP_ID, plan.tripId());
            assertEquals(DISRUPTION_ID, plan.disruptionId());
            assertEquals(NOW, plan.steps().getFirst().expectedStartAt());
            for (int index = 0; index < plan.steps().size(); index++) {
                RecoveryPlanStep step = plan.steps().get(index);
                assertEquals(index + 1, step.sequence());
                assertTrue(step.expectedCompleteAt().isAfter(step.expectedStartAt()));
                if (index > 0) {
                    assertEquals(plan.steps().get(index - 1).expectedCompleteAt(), step.expectedStartAt());
                }
            }
        }
    }

    @Test
    void emergencyFallbackRemainsWhenNoNormalResourceExists() {
        RecoveryPlanGenerationResult result = generator.generate(
                context(List.of(), List.of()), DISRUPTION_ID, overheating()
        );

        assertEquals(1, result.plans().size());
        assertEquals(0, result.normalPlanCount());
        RecoveryPlan emergency = result.plans().getFirst();
        assertEquals(RecoveryPlanType.EMERGENCY_ESCALATION, emergency.type());
        assertNull(emergency.providerId());
        assertNull(emergency.safeStopId());
        assertEquals(RecoveryPlanGenerator.WORKSHOP_PROVIDER_MISSING,
                result.skippedTemplates().get(RecoveryPlanType.WORKSHOP_REPAIR));
        assertEquals(RecoveryPlanGenerator.MOBILE_MECHANIC_PROVIDER_MISSING,
                result.skippedTemplates().get(RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC));
        assertEquals(RecoveryPlanGenerator.TOWING_PROVIDER_MISSING,
                result.skippedTemplates().get(RecoveryPlanType.TOW_REPAIR_OR_TRANSFER));
        assertEquals(RecoveryPlanGenerator.SUBSTITUTE_VEHICLE_PROVIDER_MISSING,
                result.skippedTemplates().get(RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER));
        assertEquals(RecoveryPlanGenerator.DISRUPTION_NOT_APPLICABLE,
                result.skippedTemplates().get(RecoveryPlanType.DRIVER_REST));
    }

    @Test
    void missingSafeStopExplainsWhyMobileMechanicTemplateWasSkipped() {
        RecoveryPlanGenerationResult result = generator.generate(
                context(List.of(provider(MOBILE_MECHANIC, ProviderType.MOBILE_MECHANIC)), List.of()),
                DISRUPTION_ID,
                overheating()
        );

        assertEquals(RecoveryPlanGenerator.SAFE_STOP_MISSING,
                result.skippedTemplates().get(RecoveryPlanType.SAFE_PARKING_MOBILE_MECHANIC));
    }

    @Test
    void substituteVehicleResourceCreatesTransferCandidate() {
        UUID substituteId = id("00000000-0000-4000-8700-000000000006");
        RecoveryPlanGenerationResult result = generator.generate(
                context(List.of(provider(substituteId, ProviderType.SUBSTITUTE_VEHICLE)), List.of()),
                DISRUPTION_ID,
                overheating()
        );

        RecoveryPlan transfer = result.plans().stream()
                .filter(plan -> plan.type() == RecoveryPlanType.SUBSTITUTE_VEHICLE_TRANSFER)
                .findFirst()
                .orElseThrow();
        assertEquals(substituteId, transfer.providerId());
        assertEquals("TRANSFER_CARGO_TO_SUBSTITUTE", transfer.steps().getLast().actionCode());
    }

    @Test
    void rejectsDisruptionFromDifferentContextSnapshot() {
        DetectedDisruption mismatched = new DetectedDisruption(
                DisruptionType.VEHICLE_OVERHEATING,
                DisruptionSeverity.HIGH,
                NOW,
                List.of(),
                Map.of("contextHash", "d".repeat(64))
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                generator.generate(context(providers(), safeStops()), DISRUPTION_ID, mismatched));

        assertEquals("Disruption evidence does not match the trip context hash", exception.getMessage());
    }

    @Test
    void rejectsUnsupportedDetectedDisruptionType() {
        DetectedDisruption cargoDeviation = new DetectedDisruption(
                DisruptionType.CARGO_TEMPERATURE_DEVIATION,
                DisruptionSeverity.HIGH,
                NOW,
                List.of("CARGO_TEMPERATURE_AT_RISK"),
                Map.of("contextHash", HASH)
        );

        assertThrows(IllegalArgumentException.class, () ->
                generator.generate(context(providers(), safeStops()), DISRUPTION_ID, cargoDeviation));
    }

    private static DetectedDisruption overheating() {
        return new DetectedDisruption(
                DisruptionType.VEHICLE_OVERHEATING,
                DisruptionSeverity.HIGH,
                NOW,
                List.of(),
                Map.of("contextHash", HASH, "ruleId", "VEHICLE-OVERHEAT-DETECT-001")
        );
    }

    private static TripContext context(List<ProviderContext> providers, List<SafeStopContext> safeStops) {
        UUID vehicleId = id("00000000-0000-4000-8300-000000000001");
        ObservationMetadata fresh = observation(NOW.plusSeconds(300), ObservationStatus.FRESH);
        return new TripContext(
                new Trip(
                        TRIP_ID,
                        id("00000000-0000-4000-8400-000000000001"),
                        vehicleId,
                        id("00000000-0000-4000-8500-000000000001"),
                        id("00000000-0000-4000-8500-000000000002"),
                        id("00000000-0000-4000-8500-000000000003"),
                        "TRIP-OVERHEAT-1",
                        TripStatus.DISRUPTED,
                        NOW.plusSeconds(3600),
                        60,
                        30,
                        NOW.minusSeconds(600),
                        NOW.minusSeconds(700),
                        NOW.minusSeconds(5)
                ),
                new VehicleContext(
                        vehicleId,
                        "TN-NEEPO-01",
                        "REFRIGERATED_TRUCK",
                        VehicleStatus.DISRUPTED,
                        decimal("3.5"),
                        decimal("2.5"),
                        decimal("12000"),
                        decimal("5000"),
                        decimal("105"),
                        decimal("112"),
                        decimal("70"),
                        true,
                        List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR"),
                        fresh
                ),
                new DriverContext(
                        id("00000000-0000-4000-8500-000000000001"),
                        DriverStatus.DRIVING,
                        300,
                        540,
                        NOW.minusSeconds(3600),
                        fresh
                ),
                new CargoContext(
                        id("00000000-0000-4000-8500-000000000002"),
                        CargoType.REFRIGERATED,
                        decimal("2500"),
                        decimal("2"),
                        decimal("8"),
                        decimal("6"),
                        false,
                        List.of("MAINTAIN_TEMPERATURE"),
                        fresh
                ),
                new RouteContext(
                        id("00000000-0000-4000-8500-000000000003"),
                        RouteStatus.ACTIVE,
                        new GeoPoint(11.0, 77.0),
                        new GeoPoint(12.0, 78.0),
                        new GeoPoint(11.4, 77.6),
                        decimal("80"),
                        60,
                        List.of(),
                        fresh
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

    private static List<ProviderContext> providers() {
        return List.of(
                provider(WORKSHOP_C, ProviderType.WORKSHOP, ProviderAvailabilityStatus.AVAILABLE,
                        List.of("REFRIGERATED_HEAVY_VEHICLE_REPAIR"), observation(NOW, ObservationStatus.FRESH)),
                provider(TOWING_SERVICE, ProviderType.TOWING_SERVICE),
                provider(WORKSHOP_A, ProviderType.WORKSHOP, ProviderAvailabilityStatus.UNAVAILABLE,
                        List.of("HEAVY_VEHICLE_REPAIR"), observation(NOW.plusSeconds(300), ObservationStatus.FRESH)),
                provider(MOBILE_MECHANIC, ProviderType.MOBILE_MECHANIC),
                provider(WORKSHOP_B, ProviderType.WORKSHOP)
        );
    }

    private static List<SafeStopContext> safeStops() {
        ObservationMetadata fresh = observation(NOW.plusSeconds(300), ObservationStatus.FRESH);
        return List.of(
                new SafeStopContext(SAFE_STOP_B, "Safe Stop B", new GeoPoint(11.6, 77.7),
                        decimal("4.5"), decimal("15000"), true, fresh),
                new SafeStopContext(SAFE_STOP_A, "Safe Stop A", new GeoPoint(11.5, 77.6),
                        decimal("4"), decimal("14000"), true, fresh)
        );
    }

    private static ProviderContext provider(UUID providerId, ProviderType providerType) {
        return provider(
                providerId,
                providerType,
                ProviderAvailabilityStatus.AVAILABLE,
                List.of(providerType.name()),
                observation(NOW.plusSeconds(300), ObservationStatus.FRESH)
        );
    }

    private static ProviderContext provider(
            UUID providerId,
            ProviderType providerType,
            ProviderAvailabilityStatus availability,
            List<String> capabilities,
            ObservationMetadata observation
    ) {
        return new ProviderContext(
                providerId,
                providerType + " " + providerId,
                providerType,
                new GeoPoint(11.5, 77.7),
                capabilities,
                availability,
                45,
                decimal("0.9"),
                true,
                observation
        );
    }

    private static ObservationMetadata observation(Instant expiresAt, ObservationStatus status) {
        return new ObservationMetadata(
                NOW.minusSeconds(30),
                NOW.minusSeconds(29),
                expiresAt,
                0.95,
                status,
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
