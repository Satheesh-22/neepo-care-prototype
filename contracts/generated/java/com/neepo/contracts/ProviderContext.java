package com.neepo.contracts;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProviderContext(
        UUID providerId,
        String name,
        ProviderType providerType,
        GeoPoint location,
        List<String> capabilities,
        ProviderAvailabilityStatus availabilityStatus,
        Integer estimatedArrivalMinutes,
        BigDecimal reliabilityScore,
        boolean requiredResourceAvailable,
        ObservationMetadata availabilityObservation
) {
}
