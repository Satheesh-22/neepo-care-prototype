package com.neepo.contracts;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CargoContext(
        UUID cargoId,
        CargoType cargoType,
        BigDecimal weightKg,
        BigDecimal minimumTemperatureCelsius,
        BigDecimal maximumTemperatureCelsius,
        BigDecimal currentTemperatureCelsius,
        boolean hazardous,
        List<String> handlingRequirements,
        ObservationMetadata conditionObservation
) {
}
