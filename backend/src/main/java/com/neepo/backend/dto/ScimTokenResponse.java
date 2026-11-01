package com.neepo.backend.dto;

import java.time.Instant;

public record ScimTokenResponse(
        String token,
        String prefix,
        String scimBaseUrl,
        boolean enabled,
        Instant generatedAt
) {}

