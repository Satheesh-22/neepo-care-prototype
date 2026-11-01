package com.neepo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record RegisterHardwareGatewayRequest(
        @NotBlank @Size(max = 64) String deviceSerial,
        UUID vehicleId,
        @Size(max = 32) String imei,
        @Size(max = 64) String secureElementId,
        @Size(max = 32) String firmwareVersion,
        @NotBlank String hmacSecret,
        Integer bufferCapacityRecords,
        Integer reportingIntervalSeconds,
        Map<String, Object> metadata
) {}
