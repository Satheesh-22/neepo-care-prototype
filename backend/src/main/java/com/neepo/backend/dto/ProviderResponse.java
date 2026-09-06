package com.neepo.backend.dto;

import com.neepo.backend.domain.ProviderType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProviderResponse(
        UUID id,
        String name,
        ProviderType providerType,
        GeoPointResponse location,
        BigDecimal serviceRadiusKm,
        BigDecimal reliabilityScore,
        boolean active,
        List<String> capabilities,
        ProviderAvailabilityResponse latestAvailability
) {
    public ProviderResponse {
        capabilities = List.copyOf(capabilities);
    }
}
