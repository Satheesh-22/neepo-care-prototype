package com.neepo.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @NotBlank(message = "Legal name is required")
        @Size(max = 150, message = "Legal name must not exceed 150 characters")
        String legalName,

        @Size(max = 150, message = "Trading name must not exceed 150 characters")
        String tradingName,

        @NotBlank(message = "Registration number is required")
        @Size(max = 80, message = "Registration number must not exceed 80 characters")
        String registrationNumber,

        @Size(max = 80, message = "Industry must not exceed 80 characters")
        String industry,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @Size(max = 100, message = "State or province must not exceed 100 characters")
        String stateProvince,

        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        @Size(max = 30, message = "Postal code must not exceed 30 characters")
        String postalCode,

        @Size(max = 100, message = "Contact person must not exceed 100 characters")
        String contactPerson,

        @NotBlank(message = "Business email is required")
        @Email(message = "Business email must be valid")
        @Size(max = 150, message = "Business email must not exceed 150 characters")
        String businessEmail,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must match +?[0-9]{10,15}")
        String phoneNumber,

        @Size(max = 60, message = "Timezone must not exceed 60 characters")
        String timezone,

        @Pattern(regexp = "^(METRIC|IMPERIAL)$", message = "Measurement units must be METRIC or IMPERIAL")
        String measurementUnits,

        @Size(max = 60, message = "Cargo temperature policy must not exceed 60 characters")
        String cargoTemperaturePolicy,

        @Size(max = 255, message = "Logo reference must not exceed 255 characters")
        String logoReference,

        Integer dataRetentionDays
) {
}
