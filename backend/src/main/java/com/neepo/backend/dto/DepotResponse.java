package com.neepo.backend.dto;

import com.neepo.backend.domain.DepotStatus;
import java.time.Instant;
import java.util.UUID;

public record DepotResponse(
        UUID id,
        UUID companyId,
        String code,
        String name,
        String address,
        String city,
        String state,
        String postalCode,
        Double latitude,
        Double longitude,
        Integer capacity,
        DepotStatus status,
        String contactPhone,
        String contactEmail,
        long assignedVehiclesCount,
        long assignedDriversCount,
        Instant createdAt,
        Instant updatedAt
) {}

