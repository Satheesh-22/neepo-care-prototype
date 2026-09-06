package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.CreateVehicleRequest;
import com.neepo.backend.dto.VehicleResponse;
import com.neepo.backend.service.VehicleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {
    private final VehicleService vehicles;
    public VehicleController(VehicleService vehicles) { this.vehicles = vehicles; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VehicleResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody CreateVehicleRequest request,
                                                HttpServletRequest httpRequest) {
        return ApiResponse.success(vehicles.create(TenantScope.companyId(jwt), request),
                TraceIdFilter.traceId(httpRequest));
    }

    @GetMapping("/{vehicleId}")
    public ApiResponse<VehicleResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID vehicleId,
                                             HttpServletRequest httpRequest) {
        return ApiResponse.success(vehicles.get(TenantScope.companyId(jwt), vehicleId),
                TraceIdFilter.traceId(httpRequest));
    }
}
