package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.CargoResponse;
import com.neepo.backend.dto.CreateCargoRequest;
import com.neepo.backend.service.CargoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cargo")
public class CargoController {
    private final CargoService cargo;
    public CargoController(CargoService cargo) { this.cargo = cargo; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CargoResponse> create(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody CreateCargoRequest request,
                                              HttpServletRequest httpRequest) {
        return ApiResponse.success(cargo.create(TenantScope.companyId(jwt), request),
                TraceIdFilter.traceId(httpRequest));
    }

    @GetMapping("/{cargoId}")
    public ApiResponse<CargoResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID cargoId,
                                           HttpServletRequest httpRequest) {
        return ApiResponse.success(cargo.get(TenantScope.companyId(jwt), cargoId),
                TraceIdFilter.traceId(httpRequest));
    }
}
