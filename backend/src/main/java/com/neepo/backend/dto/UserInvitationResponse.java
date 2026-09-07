package com.neepo.backend.dto;

import com.neepo.backend.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserInvitationResponse(
        UUID id,
        UUID userId,
        String email,
        UserRole role,
        String token,
        Instant expiresAt,
        Instant acceptedAt,
        Instant createdAt
) {
}
