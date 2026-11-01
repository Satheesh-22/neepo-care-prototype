package com.neepo.backend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SsoAuditLogResponse(
        UUID id,
        UUID companyId,
        String userEmail,
        String idpType,
        String eventType,
        String ipAddress,
        String userAgent,
        Map<String, Object> details,
        Instant createdAt
) {}
