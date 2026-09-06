package com.neepo.backend.dto;

import com.neepo.backend.domain.DriverStatus;

import java.time.Instant;
import java.util.UUID;

public record DriverResponse(
        UUID id, UUID userId, String licenseNumber, DriverStatus status,
        int maximumDailyDrivingMinutes, int remainingDrivingMinutes,
        Instant lastRestCompletedAt, Instant createdAt, Instant updatedAt
) {
}
