package com.neepo.backend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CompanySsoConfigResponse(
        UUID id,
        UUID companyId,
        String idpType,
        String idpIssuerUrl,
        String clientId,
        String ssoDomain,
        boolean enabled,
        boolean enforceSso,
        boolean jitProvisioningEnabled,
        String defaultRole,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt
) {}
