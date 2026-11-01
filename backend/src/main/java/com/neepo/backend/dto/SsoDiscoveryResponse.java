package com.neepo.backend.dto;

import java.util.UUID;

public record SsoDiscoveryResponse(
        boolean ssoAvailable,
        String domain,
        UUID companyId,
        String companyName,
        String idpType,
        String idpIssuerUrl,
        String authorizationEndpoint,
        boolean enforceSso
) {}
