package com.neepo.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record DriverVehicleAssignmentResponse(
        UUID id,
        UUID companyId,
        UUID driverId,
        String driverName,
        String driverLicenseNumber,
        String driverEmployeeId,
        UUID vehicleId,
        String vehicleRegistrationNumber,
        String vehicleType,
        Instant startTime,
        Instant endTime,
        String notes,
        String status,
        UUID assignedByUserId,
        Instant createdAt,
        Instant releasedAt
) {
}
