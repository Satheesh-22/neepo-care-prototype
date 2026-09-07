package com.neepo.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record DriverVehicleAssignmentRequest(
        @NotNull(message = "Driver ID is required")
        UUID driverId,

        @NotNull(message = "Vehicle ID is required")
        UUID vehicleId,

        Instant startTime,

        Instant endTime,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes
) {
}
