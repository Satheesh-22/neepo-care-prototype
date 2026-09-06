package com.neepo.backend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SystemEventMessage(
        UUID eventId,
        String type,
        UUID tripId,
        UUID aggregateId,
        Instant occurredAt,
        int schemaVersion,
        Map<String, Object> payload
) {
}
