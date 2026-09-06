package com.neepo.contracts;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VehicleContext(
        UUID vehicleId,
        String registrationNumber,
        String vehicleType,
        VehicleStatus status,
        BigDecimal heightMeters,
        BigDecimal widthMeters,
        BigDecimal grossWeightKg,
        BigDecimal payloadCapacityKg,
        BigDecimal maximumEngineTemperatureCelsius,
        BigDecimal currentEngineTemperatureCelsius,
        BigDecimal engineHealthPercent,
        boolean refrigerationSupported,
        List<String> requiredCapabilities,
        ObservationMetadata healthObservation
) {
}
