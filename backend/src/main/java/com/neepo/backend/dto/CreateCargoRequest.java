package com.neepo.backend.dto;

import com.neepo.backend.domain.CargoType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateCargoRequest(
        @NotBlank @Size(max = 50) String referenceCode,
        @NotNull CargoType cargoType,
        @NotBlank @Size(max = 300) String description,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal weightKg,
        BigDecimal minimumTemperatureCelsius,
        BigDecimal maximumTemperatureCelsius,
        @NotNull Boolean hazardous
) {
    @AssertTrue(message = "Minimum temperature must not exceed maximum temperature")
    public boolean isTemperatureRangeValid() {
        return minimumTemperatureCelsius == null || maximumTemperatureCelsius == null
                || minimumTemperatureCelsius.compareTo(maximumTemperatureCelsius) <= 0;
    }
}
