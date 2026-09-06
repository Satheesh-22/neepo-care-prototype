package com.neepo.backend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateVehicleRequest(
        @NotBlank @Size(max = 30) String registrationNumber,
        @NotBlank @Size(max = 60) String vehicleType,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal heightMeters,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal widthMeters,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal grossWeightKg,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal payloadCapacityKg,
        @NotNull @DecimalMin("-50") @DecimalMax("250") BigDecimal maximumEngineTemperatureCelsius,
        @NotNull Boolean refrigerationSupported
) {
}
