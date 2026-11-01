package com.neepo.backend.dto;

public record ScimStatusResponse(
        boolean enabled,
        String tokenPrefix,
        String scimBaseUrl,
        long provisionedUsersCount
) {}

