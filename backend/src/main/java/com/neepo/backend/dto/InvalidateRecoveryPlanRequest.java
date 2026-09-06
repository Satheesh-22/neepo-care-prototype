package com.neepo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record InvalidateRecoveryPlanRequest(
        @NotNull UUID eventId,
        @NotBlank String explanationCode,
        @NotNull Instant observedAt,
        @Size(max = 500) String details
) {
}
