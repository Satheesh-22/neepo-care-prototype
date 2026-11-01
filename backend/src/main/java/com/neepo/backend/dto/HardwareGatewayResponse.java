package com.neepo.backend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record HardwareGatewayResponse(
        UUID id,
        UUID companyId,
        UUID vehicleId,
        String vehicleRegistrationNumber,
        String deviceSerial,
        String imei,
        String secureElementId,
        String firmwareVersion,
        String status,
        Instant lastHeartbeatAt,
        UUID lastSyncEventId,
        Integer bufferCapacityRecords,
        Integer reportingIntervalSeconds,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {}
