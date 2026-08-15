package com.neepo.contracts;

import java.time.Instant;
import java.util.UUID;

public record DriverContext(
        UUID driverId,
        DriverStatus status,
        int remainingDrivingMinutes,
        int maximumDailyDrivingMinutes,
        Instant lastRestCompletedAt,
        ObservationMetadata statusObservation
) {
}
