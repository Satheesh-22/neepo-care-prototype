package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.CreateDriverRequest;
import com.neepo.backend.dto.DriverResponse;
import com.neepo.backend.service.DriverService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {
    private final DriverService drivers;
    public DriverController(DriverService drivers) { this.drivers = drivers; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DriverResponse> create(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateDriverRequest request,
                                               HttpServletRequest httpRequest) {
        return ApiResponse.success(drivers.create(TenantScope.companyId(jwt), request),
                TraceIdFilter.traceId(httpRequest));
    }

    @GetMapping("/{driverId}")
    public ApiResponse<DriverResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID driverId,
                                            HttpServletRequest httpRequest) {
        return ApiResponse.success(drivers.get(TenantScope.companyId(jwt), driverId),
                TraceIdFilter.traceId(httpRequest));
    }
}
