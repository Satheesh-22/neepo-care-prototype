package com.neepo.backend.controller;

import com.neepo.backend.dto.ScimStatusResponse;
import com.neepo.backend.dto.ScimTokenResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.service.ScimService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/scim")
@PreAuthorize("hasRole('ADMIN')")
public class ScimAdminController {

    private final ScimService scimService;

    public ScimAdminController(ScimService scimService) {
        this.scimService = scimService;
    }

    @GetMapping("/status")
    public ResponseEntity<ScimStatusResponse> getStatus(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        String baseUrl = extractBaseUrl(request);
        return ResponseEntity.ok(scimService.getScimStatus(companyId, baseUrl));
    }

    @PostMapping("/token")
    public ResponseEntity<ScimTokenResponse> generateToken(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        String baseUrl = extractBaseUrl(request);
        return ResponseEntity.ok(scimService.generateScimToken(companyId, baseUrl));
    }

    @DeleteMapping("/token")
    public ResponseEntity<Void> revokeToken(@AuthenticationPrincipal Jwt jwt) {
        UUID companyId = TenantScope.companyId(jwt);
        scimService.revokeScimToken(companyId);
        return ResponseEntity.noContent().build();
    }

    private String extractBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();
        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);

        return isDefaultPort ? scheme + "://" + serverName : scheme + "://" + serverName + ":" + port;
    }
}
