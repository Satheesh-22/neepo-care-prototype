package com.neepo.backend.dto;

public record GatewayHeartbeatRequest(
        String firmwareVersion,
        Double batteryVoltage,
        Integer signalRssi,
        Integer bufferedRecordsCount
) {}
