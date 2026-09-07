package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.domain.UserRole;
import com.neepo.backend.dto.AuditEventResponse;
import com.neepo.backend.dto.CompanyResponse;
import com.neepo.backend.dto.UpdateCompanyRequest;
import com.neepo.backend.service.AuditService;
import com.neepo.backend.service.CompanyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/company")
public class CompanyController {

    private final CompanyService companyService;
    private final AuditService auditService;

    public CompanyController(CompanyService companyService, AuditService auditService) {
        this.companyService = companyService;
        this.auditService = auditService;
    }

    @GetMapping({"", "/me"})
    public ApiResponse<CompanyResponse> getCompany(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        String roleStr = jwt.getClaimAsString("role");
        UserRole role = roleStr != null ? UserRole.valueOf(roleStr) : UserRole.DRIVER;
        return ApiResponse.success(
                companyService.getCompany(companyId, role),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PatchMapping({"", "/me"})
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CompanyResponse> updateCompanyPatch(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCompanyRequest request,
            HttpServletRequest httpRequest
    ) {
        return updateCompany(jwt, request, httpRequest);
    }

    @PutMapping({"", "/me"})
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CompanyResponse> updateCompanyPut(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCompanyRequest request,
            HttpServletRequest httpRequest
    ) {
        return updateCompany(jwt, request, httpRequest);
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AuditEventResponse>> getCompanyAudit(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(
                auditService.getEventsForAggregate("COMPANY", companyId, 20),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    private ApiResponse<CompanyResponse> updateCompany(Jwt jwt, UpdateCompanyRequest request, HttpServletRequest httpRequest) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                companyService.updateCompany(companyId, actorUserId, traceId, request),
                TraceIdFilter.traceId(httpRequest)
        );
    }
}
