package com.neepo.care.planning;

import java.time.Duration;
import java.util.Objects;

/**
 * Explicit demo timing assumptions used only to create comparable candidate timelines.
 * Provider ETA and other observed facts remain untouched for later evaluation.
 */
public record RecoveryPlanGenerationPolicy(
        Duration stabilizationDuration,
        Duration travelDuration,
        Duration workshopRepairDuration,
        Duration parkingDuration,
        Duration mobileRepairDuration,
        Duration towPreparationDuration,
        Duration towingDuration,
        Duration transferPreparationDuration,
        Duration cargoTransferDuration,
        Duration emergencyEscalationDuration
) {
    public RecoveryPlanGenerationPolicy {
        requirePositive(stabilizationDuration, "Stabilization duration");
        requirePositive(travelDuration, "Travel duration");
        requirePositive(workshopRepairDuration, "Workshop repair duration");
        requirePositive(parkingDuration, "Parking duration");
        requirePositive(mobileRepairDuration, "Mobile repair duration");
        requirePositive(towPreparationDuration, "Tow preparation duration");
        requirePositive(towingDuration, "Towing duration");
        requirePositive(transferPreparationDuration, "Transfer preparation duration");
        requirePositive(cargoTransferDuration, "Cargo transfer duration");
        requirePositive(emergencyEscalationDuration, "Emergency escalation duration");
    }

    public static RecoveryPlanGenerationPolicy demoDefaults() {
        return new RecoveryPlanGenerationPolicy(
                Duration.ofMinutes(5),
                Duration.ofMinutes(30),
                Duration.ofMinutes(120),
                Duration.ofMinutes(5),
                Duration.ofMinutes(90),
                Duration.ofMinutes(10),
                Duration.ofMinutes(60),
                Duration.ofMinutes(20),
                Duration.ofMinutes(45),
                Duration.ofMinutes(10)
        );
    }

    private static void requirePositive(Duration duration, String label) {
        Objects.requireNonNull(duration, label + " is required");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(label + " must be positive");
        }
    }
}
