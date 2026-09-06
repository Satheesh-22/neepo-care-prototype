package com.neepo.backend.dto;

import com.neepo.backend.domain.ProviderAvailabilityStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProviderAvailabilityResponse(
        ProviderAvailabilityStatus status,
        Instant observedAt,
        Instant expiresAt,
        Integer estimatedArrivalMinutes,
        BigDecimal confidence,
        boolean requiredResourceAvailable
) {
}
