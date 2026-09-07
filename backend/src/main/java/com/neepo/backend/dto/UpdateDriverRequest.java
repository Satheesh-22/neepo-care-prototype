package com.neepo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateDriverRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must match +?[0-9]{10,15}")
        String phone,

        @NotBlank(message = "Employee ID is required")
        @Size(max = 50, message = "Employee ID must not exceed 50 characters")
        String employeeId,

        @NotBlank(message = "License number is required")
        @Size(max = 50, message = "License number must not exceed 50 characters")
        String licenseNumber,

        @Size(max = 30, message = "License category must not exceed 30 characters")
        String licenseCategory,

        LocalDate licenseExpiryDate,

        @Size(max = 100, message = "Emergency contact name must not exceed 100 characters")
        String emergencyContactName,

        @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "Emergency contact phone must match +?[0-9]{10,15}")
        String emergencyContactPhone,

        @Size(max = 150, message = "Base location must not exceed 150 characters")
        String baseLocation,

        String employmentStatus,

        @Size(max = 255, message = "Profile photo URL must not exceed 255 characters")
        String profilePhotoUrl,

        Integer maximumDailyDrivingMinutes
) {
}
