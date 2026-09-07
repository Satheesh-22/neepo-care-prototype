package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.CreateVehicleDetailedRequest;
import com.neepo.backend.dto.CreateVehicleRequest;
import com.neepo.backend.dto.UpdateVehicleRequest;
import com.neepo.backend.dto.UpdateVehicleStatusRequest;
import com.neepo.backend.dto.VehicleDetailedResponse;
import com.neepo.backend.dto.VehicleResponse;
import com.neepo.backend.service.VehicleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {
    private final VehicleService vehicles;

    public VehicleController(VehicleService vehicles) {
        this.vehicles = vehicles;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VehicleDetailedResponse> createDetailed(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateVehicleDetailedRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                vehicles.createDetailed(companyId, actorUserId, traceId, request),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/simple")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VehicleResponse> createSimple(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateVehicleRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                vehicles.create(TenantScope.companyId(jwt), request),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping
    public ApiResponse<List<VehicleDetailedResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                vehicles.listVehicles(TenantScope.companyId(jwt)),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping("/{vehicleId}")
    public ApiResponse<VehicleDetailedResponse> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID vehicleId,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                vehicles.getDetailed(TenantScope.companyId(jwt), vehicleId),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PatchMapping("/{vehicleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VehicleDetailedResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                vehicles.updateVehicle(companyId, vehicleId, actorUserId, traceId, request),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @org.springframework.web.bind.annotation.RequestMapping(
            value = "/{vehicleId}/status",
            method = {
                    org.springframework.web.bind.annotation.RequestMethod.POST,
                    org.springframework.web.bind.annotation.RequestMethod.PUT,
                    org.springframework.web.bind.annotation.RequestMethod.PATCH
            }
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<VehicleDetailedResponse> updateStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleStatusRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                vehicles.updateStatus(companyId, vehicleId, actorUserId, traceId, request.status()),
                TraceIdFilter.traceId(httpRequest)
        );
    }
}
