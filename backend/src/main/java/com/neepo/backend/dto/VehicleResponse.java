package com.neepo.backend.dto;

import com.neepo.backend.domain.VehicleStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VehicleResponse(
        UUID id, String registrationNumber, String vehicleType,
        BigDecimal heightMeters, BigDecimal widthMeters, BigDecimal grossWeightKg,
        BigDecimal payloadCapacityKg, BigDecimal maximumEngineTemperatureCelsius,
        boolean refrigerationSupported, VehicleStatus status, BigDecimal currentEngineHealthPercent,
        Instant createdAt, Instant updatedAt
) {
}
