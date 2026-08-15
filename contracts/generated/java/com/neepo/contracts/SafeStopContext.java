package com.neepo.contracts;

import java.math.BigDecimal;
import java.util.UUID;

public record SafeStopContext(
        UUID safeStopId,
        String name,
        GeoPoint location,
        BigDecimal maximumVehicleHeightMeters,
        BigDecimal maximumVehicleWeightKg,
        boolean securityVerified,
        ObservationMetadata observation
) {
}
