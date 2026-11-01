package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.*;
import com.neepo.backend.service.HardwareGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gateways")
public class HardwareGatewayController {

    private final HardwareGatewayService gatewayService;

    public HardwareGatewayController(HardwareGatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<List<HardwareGatewayResponse>> listGateways(@AuthenticationPrincipal Jwt jwt,
                                                                   HttpServletRequest httpRequest) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(gatewayService.listGateways(companyId), TraceIdFilter.traceId(httpRequest));
    }

    @GetMapping("/{gatewayId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<HardwareGatewayResponse> getGateway(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable UUID gatewayId,
                                                           HttpServletRequest httpRequest) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(gatewayService.getGateway(companyId, gatewayId), TraceIdFilter.traceId(httpRequest));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<HardwareGatewayResponse> registerGateway(@AuthenticationPrincipal Jwt jwt,
                                                                @Valid @RequestBody RegisterHardwareGatewayRequest request,
                                                                HttpServletRequest httpRequest) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(gatewayService.registerGateway(companyId, request), TraceIdFilter.traceId(httpRequest));
    }

    @GetMapping("/{gatewayId}/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<GatewayOtaConfigResponse> getOtaConfig(@AuthenticationPrincipal Jwt jwt,
                                                             @PathVariable UUID gatewayId,
                                                             HttpServletRequest httpRequest) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(gatewayService.getOtaConfig(companyId, gatewayId), TraceIdFilter.traceId(httpRequest));
    }

    @PostMapping("/{gatewayId}/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<GatewayBatchSyncResponse> syncBatch(@AuthenticationPrincipal Jwt jwt,
                                                           @PathVariable UUID gatewayId,
                                                           @RequestHeader(value = "X-Device-Signature", required = false) String signature,
                                                           @RequestHeader(value = "X-Device-Timestamp", required = false) String timestamp,
                                                           @Valid @RequestBody GatewayBatchSyncRequest request,
                                                           HttpServletRequest httpRequest) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(
                gatewayService.syncBatch(companyId, gatewayId, request, signature, timestamp),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/{gatewayId}/heartbeat")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<GatewayHeartbeatResponse> recordHeartbeat(@AuthenticationPrincipal Jwt jwt,
                                                                 @PathVariable UUID gatewayId,
                                                                 @RequestBody(required = false) GatewayHeartbeatRequest request,
                                                                 HttpServletRequest httpRequest) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(
                gatewayService.recordHeartbeat(companyId, gatewayId, request),
                TraceIdFilter.traceId(httpRequest)
        );
    }
}
