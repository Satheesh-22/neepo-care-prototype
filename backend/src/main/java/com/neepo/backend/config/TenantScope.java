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
        String value = jwt.getSubject();
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Authenticated token has no user subject");
        }
        return UUID.fromString(value);
    }

    public static boolean isDriver(Jwt jwt) {
        return "DRIVER".equals(jwt.getClaimAsString("role"));
    }
}
