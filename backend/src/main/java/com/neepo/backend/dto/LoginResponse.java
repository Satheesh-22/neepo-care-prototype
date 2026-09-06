package com.neepo.backend.dto;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        Instant expiresAt,
        UserResponse user
) {
}
