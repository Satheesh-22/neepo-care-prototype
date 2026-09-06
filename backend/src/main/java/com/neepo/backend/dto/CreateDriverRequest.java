package com.neepo.backend.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateDriverRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 50) String licenseNumber,
        @Min(1) @Max(1440) int maximumDailyDrivingMinutes
) {
}
