package com.neepo.backend.dto;

import com.neepo.backend.domain.CargoType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CargoResponse(
        UUID id, String referenceCode, CargoType cargoType, String description, BigDecimal weightKg,
        BigDecimal minimumTemperatureCelsius, BigDecimal maximumTemperatureCelsius, boolean hazardous,
        Instant createdAt, Instant updatedAt
) {
}
