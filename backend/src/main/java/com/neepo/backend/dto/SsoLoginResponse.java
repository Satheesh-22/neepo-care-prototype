package com.neepo.backend.dto;

import java.time.Instant;

public record SsoLoginResponse(
        String accessToken,
        Instant expiresAt,
        UserResponse user,
        boolean isNewUser,
        String idpType
) {}
