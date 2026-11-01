package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.CompanySsoConfigResponse;
import com.neepo.backend.dto.SsoAuditLogResponse;
import com.neepo.backend.dto.SsoTestResultResponse;
import com.neepo.backend.dto.UpdateCompanySsoConfigRequest;
import com.neepo.backend.service.SsoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/sso")
@PreAuthorize("hasRole('ADMIN')")
public class SsoAdminController {

    private final SsoService ssoService;

    public SsoAdminController(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    @GetMapping
    public ApiResponse<CompanySsoConfigResponse> getConfig(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(ssoService.getCompanySsoConfig(companyId), TraceIdFilter.traceId(httpRequest));
    }

    @PutMapping
    public ApiResponse<CompanySsoConfigResponse> updateConfig(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCompanySsoConfigRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        String actorEmail = jwt.getClaimAsString("email");
        return ApiResponse.success(
                ssoService.updateCompanySsoConfig(companyId, request, actorUserId, actorEmail),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping("/audit-logs")
    public ApiResponse<List<SsoAuditLogResponse>> listAuditLogs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(ssoService.listAuditLogs(companyId, limit), TraceIdFilter.traceId(httpRequest));
    }

    @PostMapping("/test")
    public ApiResponse<SsoTestResultResponse> testConfig(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(ssoService.testSsoConfiguration(companyId), TraceIdFilter.traceId(httpRequest));
    }
}

