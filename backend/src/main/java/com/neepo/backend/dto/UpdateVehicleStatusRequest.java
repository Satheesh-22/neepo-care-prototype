package com.neepo.backend.dto;

import com.neepo.backend.domain.VehicleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateVehicleStatusRequest(
        @NotNull(message = "Vehicle status is required")
        VehicleStatus status
) {
}
