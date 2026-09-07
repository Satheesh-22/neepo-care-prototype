package com.neepo.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID traceId,
        UUID actorUserId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        String details,
        Instant createdAt
) {
}
