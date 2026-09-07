package com.neepo.backend.dto;

import com.neepo.backend.domain.DriverStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DriverDetailedResponse(
        UUID id,
        UUID userId,
        String employeeId,
        String firstName,
        String lastName,
        String name,
        String email,
        String phone,
        String licenseNumber,
        String licenseCategory,
        LocalDate licenseExpiryDate,
        boolean licenseExpired,
        String emergencyContactName,
        String emergencyContactPhone,
        String baseLocation,
        String employmentStatus,
        String profilePhotoUrl,
        String verificationState,
        DriverStatus status,
        int maximumDailyDrivingMinutes,
        int remainingDrivingMinutes,
        Instant lastRestCompletedAt,
        Instant lastActiveAt,
        UUID assignedVehicleId,
        String assignedVehicleRegistration,
        Instant createdAt,
        Instant updatedAt,
        String invitationToken
) {
}
