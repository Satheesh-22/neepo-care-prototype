package com.neepo.backend.dto;

import com.neepo.backend.domain.VehicleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateVehicleDetailedRequest(
        @NotBlank(message = "Registration number is required")
        @Size(max = 30, message = "Registration number must not exceed 30 characters")
        String registrationNumber,

        @Size(max = 50, message = "VIN must not exceed 50 characters")
        String vin,

        @Size(max = 80, message = "Make must not exceed 80 characters")
        String make,

        @Size(max = 80, message = "Model must not exceed 80 characters")
        String model,

        Integer year,

        @NotBlank(message = "Vehicle type is required")
        @Size(max = 60, message = "Vehicle type must not exceed 60 characters")
        String vehicleType,

        @NotNull(message = "Height is required")
        @DecimalMin(value = "0.1", message = "Height must be positive")
        BigDecimal heightMeters,

        @NotNull(message = "Width is required")
        @DecimalMin(value = "0.1", message = "Width must be positive")
        BigDecimal widthMeters,

        @NotNull(message = "Gross weight is required")
        @DecimalMin(value = "1.0", message = "Gross weight must be positive")
        BigDecimal grossWeightKg,

        @NotNull(message = "Payload capacity is required")
        @DecimalMin(value = "1.0", message = "Payload capacity must be positive")
        BigDecimal payloadCapacityKg,

        @NotNull(message = "Max engine temperature is required")
        BigDecimal maximumEngineTemperatureCelsius,

        Boolean refrigerationSupported,

        BigDecimal cargoTemperatureMinCelsius,

        BigDecimal cargoTemperatureMaxCelsius,

        @Size(max = 100, message = "Device identifier must not exceed 100 characters")
        String deviceIdentifier,

        VehicleStatus status
) {
}
