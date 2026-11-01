package com.neepo.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record GatewayOtaConfigResponse(
        UUID gatewayId,
        String deviceSerial,
        String firmwareVersion,
        Integer reportingIntervalSeconds,
        Integer bufferCapacityRecords,
        UUID activeTripId,
        UUID activeVehicleId,
        Double maxEngineTemperatureCelsius,
        Double minCargoTemperatureCelsius,
        Double maxCargoTemperatureCelsius,
        String ingestionEndpoint,
        Instant serverTime
) {}
