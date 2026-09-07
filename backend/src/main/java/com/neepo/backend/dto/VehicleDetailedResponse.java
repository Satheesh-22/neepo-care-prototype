package com.neepo.backend.dto;

import com.neepo.backend.domain.VehicleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VehicleDetailedResponse(
        UUID id,
        String registrationNumber,
        String vin,
        String make,
        String model,
        Integer year,
        String vehicleType,
        VehicleStatus status,
        BigDecimal heightMeters,
        BigDecimal widthMeters,
        BigDecimal grossWeightKg,
        BigDecimal payloadCapacityKg,
        BigDecimal maximumEngineTemperatureCelsius,
        boolean refrigerationSupported,
        BigDecimal cargoTemperatureMinCelsius,
        BigDecimal cargoTemperatureMaxCelsius,
        String deviceIdentifier,
        BigDecimal currentEngineHealthPercent,
        UUID assignedDriverId,
        String assignedDriverName,
        Instant createdAt,
        Instant updatedAt
) {
}
