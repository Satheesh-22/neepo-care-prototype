package com.neepo.contracts;

import java.time.Instant;
import java.util.UUID;

public record Trip(
        UUID id,
        UUID companyId,
        UUID vehicleId,
        UUID driverId,
        UUID cargoId,
        UUID routeId,
        String referenceCode,
        TripStatus status,
        Instant deliveryDeadline,
        int maximumWaitingMinutes,
        int acceptedEtaThresholdMinutes,
        Instant startedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
