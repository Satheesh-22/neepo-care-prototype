package com.neepo.backend.dto;

import com.neepo.backend.domain.ProviderAvailabilityStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record ProviderAvailabilityRequest(
        @NotNull ProviderAvailabilityStatus status,
        @NotNull Instant observedAt,
        @NotNull Instant expiresAt,
        @Min(0) Integer estimatedArrivalMinutes,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal confidence,
        @NotNull Boolean requiredResourceAvailable
) {
    @AssertTrue(message = "Availability expiresAt must be later than observedAt")
    public boolean isTimestampOrderValid() {
        return observedAt == null || expiresAt == null || expiresAt.isAfter(observedAt);
    }
}
