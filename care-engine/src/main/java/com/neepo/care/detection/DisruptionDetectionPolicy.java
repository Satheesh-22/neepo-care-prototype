package com.neepo.care.detection;

import com.neepo.contracts.DisruptionSeverity;

import java.util.Objects;

public record DisruptionDetectionPolicy(
        DisruptionSeverity vehicleOverheatingSeverity,
        DisruptionSeverity cargoTemperatureDeviationSeverity
) {
    public DisruptionDetectionPolicy {
        Objects.requireNonNull(vehicleOverheatingSeverity, "Vehicle overheating severity is required");
        Objects.requireNonNull(
                cargoTemperatureDeviationSeverity,
                "Cargo temperature deviation severity is required"
        );
    }

    public static DisruptionDetectionPolicy demoDefaults() {
        return new DisruptionDetectionPolicy(DisruptionSeverity.HIGH, DisruptionSeverity.HIGH);
    }
}
