package com.neepo.backend.dto;

import com.neepo.backend.domain.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String phone,
        UserRole role,
        Instant createdAt,
        Instant updatedAt
) {
}
