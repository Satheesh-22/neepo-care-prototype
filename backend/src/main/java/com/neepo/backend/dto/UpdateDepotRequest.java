package com.neepo.backend.dto;

import com.neepo.backend.domain.DepotStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateDepotRequest(
        @Size(max = 32) String code,
        @Size(max = 128) String name,
        @Size(max = 256) String address,
        @Size(max = 100) String city,
        @Size(max = 50) String state,
        @Size(max = 20) String postalCode,
        Double latitude,
        Double longitude,
        @Positive Integer capacity,
        DepotStatus status,
        @Size(max = 20) String contactPhone,
        @Size(max = 150) String contactEmail
) {}

