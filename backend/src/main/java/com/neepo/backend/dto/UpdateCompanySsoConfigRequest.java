package com.neepo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record UpdateCompanySsoConfigRequest(
        @NotBlank String idpType,
        @NotBlank String idpIssuerUrl,
        @NotBlank String clientId,
        String clientSecret,
        @NotBlank String ssoDomain,
        boolean enabled,
        boolean enforceSso,
        boolean jitProvisioningEnabled,
        @NotBlank String defaultRole,
        Map<String, Object> metadata
) {}
