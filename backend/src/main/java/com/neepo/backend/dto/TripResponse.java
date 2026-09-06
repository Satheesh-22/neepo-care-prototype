package com.neepo.backend.dto;

import com.neepo.backend.domain.TripStatus;

import java.time.Instant;
import java.util.UUID;

public record TripResponse(
        UUID id, UUID companyId, String referenceCode, UUID vehicleId, UUID driverId, UUID cargoId, UUID routeId,
        TripStatus status, Instant deliveryDeadline, int maximumWaitingMinutes, int acceptedEtaThresholdMinutes,
        Instant startedAt, Instant createdAt, Instant updatedAt
) {
}
