package com.neepo.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record GatewayHeartbeatResponse(
        UUID gatewayId,
        String status,
        Instant serverTime,
        Boolean otaUpdateAvailable
) {}
