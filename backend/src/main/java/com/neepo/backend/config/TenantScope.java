package com.neepo.backend.config;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class TenantScope {
    private TenantScope() {
    }

    public static UUID companyId(Jwt jwt) {
        String value = jwt.getClaimAsString("companyId");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Authenticated user is not assigned to a company");
        }
        return UUID.fromString(value);
    }

    public static UUID userId(Jwt jwt) {
        if (jwt == null) {
            return UUID.fromString("00000000-0000-4000-8000-000000000000");
        }
        String value = jwt.getSubject();
        if (value == null || value.isBlank()) {
            value = jwt.getClaimAsString("sub");
        }
        if (value == null || value.isBlank()) {
            value = jwt.getClaimAsString("userId");
        }
        if (value == null || value.isBlank()) {
            return UUID.fromString("00000000-0000-4000-8000-000000000000");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    public static boolean isDriver(Jwt jwt) {
        return "DRIVER".equals(jwt.getClaimAsString("role"));
    }
}
