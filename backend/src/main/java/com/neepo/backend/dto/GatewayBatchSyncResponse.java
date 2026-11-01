package com.neepo.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record GatewayBatchSyncResponse(
        UUID gatewayId,
        UUID tripId,
        Integer batchSize,
        Integer acceptedCount,
        Integer deduplicatedCount,
        String syncMode,
        Boolean careEvaluated,
        Instant syncedAt
) {}
