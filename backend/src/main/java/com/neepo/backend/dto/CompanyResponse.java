package com.neepo.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String code,
        String name,
        String legalName,
        String tradingName,
        String registrationNumber,
        String industry,
        String address,
        String city,
        String stateProvince,
        String country,
        String postalCode,
        String contactPerson,
        String businessEmail,
        String phoneNumber,
        String timezone,
        String measurementUnits,
        String cargoTemperaturePolicy,
        String logoReference,
        int dataRetentionDays,
        Instant createdAt,
        Instant updatedAt
) {
}
