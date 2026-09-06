package com.neepo.backend.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public record CreateTripRequest(
        @NotBlank @Size(max = 50) String referenceCode,
        @NotNull UUID vehicleId,
        @NotNull UUID driverId,
        @NotNull UUID cargoId,
        @NotNull UUID routeId,
        @NotNull @Future Instant deliveryDeadline,
        @Min(1) int maximumWaitingMinutes,
        @Min(1) int acceptedEtaThresholdMinutes
) {
}
