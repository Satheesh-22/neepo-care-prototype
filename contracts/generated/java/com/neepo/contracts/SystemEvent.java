package com.neepo.contracts;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SystemEvent(
        UUID eventId,
        SystemEventType type,
        UUID tripId,
        UUID aggregateId,
        Instant occurredAt,
        int schemaVersion,
        Map<String, Object> payload
) {
}
