package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.domain.ComplianceExportFormat;
import com.neepo.backend.domain.ComplianceExportType;
import com.neepo.backend.dto.ComplianceExportManifestResponse;
import com.neepo.backend.dto.ComplianceExportPackageResponse;
import com.neepo.backend.dto.ComplianceVerificationRequest;
import com.neepo.backend.dto.ComplianceVerificationResponse;
import com.neepo.backend.service.ComplianceExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compliance/exports")
public class ComplianceExportController {

    private final ComplianceExportService exportService;

    public ComplianceExportController(ComplianceExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/trip/{tripId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<ComplianceExportPackageResponse> generateTripExport(
            @PathVariable UUID tripId,
            @RequestParam(required = false, defaultValue = "FULL_TRIP_DOSSIER") ComplianceExportType type,
            @RequestParam(required = false, defaultValue = "JSON_LD") ComplianceExportFormat format,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID userId = TenantScope.userId(jwt);
        String traceId = TraceIdFilter.traceId(request);
        UUID traceIdUuid = parseUuidOrDefault(traceId);

        ComplianceExportPackageResponse exportPackage = exportService.generateTripExport(
                companyId,
                tripId,
                type,
                format,
                userId,
                traceIdUuid
        );

        if (response != null && exportPackage.manifest() != null) {
            response.setHeader("X-Neepo-Digest", "sha256=" + exportPackage.manifest().sha256Digest());
        }

        return ApiResponse.success(exportPackage, traceId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<List<ComplianceExportManifestResponse>> listManifests(
            @RequestParam(required = false, defaultValue = "50") int limit,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        String traceId = TraceIdFilter.traceId(request);
        List<ComplianceExportManifestResponse> manifests = exportService.listManifests(companyId, limit);
        return ApiResponse.success(manifests, traceId);
    }

    @GetMapping("/{manifestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<ComplianceExportManifestResponse> getManifest(
            @PathVariable UUID manifestId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        String traceId = TraceIdFilter.traceId(request);
        ComplianceExportManifestResponse manifest = exportService.getManifest(companyId, manifestId);
        return ApiResponse.success(manifest, traceId);
    }

    @PostMapping("/{manifestId}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<ComplianceVerificationResponse> verifyPayload(
            @PathVariable UUID manifestId,
            @Valid @RequestBody ComplianceVerificationRequest verificationRequest,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        String traceId = TraceIdFilter.traceId(request);
        ComplianceVerificationResponse result = exportService.verifyPayload(
                companyId,
                manifestId,
                verificationRequest.payload()
        );
        return ApiResponse.success(result, traceId);
    }

    private UUID parseUuidOrDefault(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(traceId);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }
}
