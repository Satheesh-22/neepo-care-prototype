package com.neepo.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.Instant;
import java.util.UUID;

public record AssignTripRequest(
        @NotNull UUID vehicleId,
        @NotNull UUID driverId,
        @NotNull @PastOrPresent Instant assignedAt
) {
}
