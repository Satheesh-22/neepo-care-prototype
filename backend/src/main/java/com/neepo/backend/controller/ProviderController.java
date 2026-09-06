package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.ProviderAvailabilityRequest;
import com.neepo.backend.dto.ProviderResponse;
import com.neepo.backend.service.ProviderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/providers")
public class ProviderController {
    private final ProviderService providers;

    public ProviderController(ProviderService providers) {
        this.providers = providers;
    }

    @GetMapping
    public ApiResponse<List<ProviderResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                providers.list(TenantScope.companyId(jwt), page, size),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/{providerId}/availability")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProviderResponse> recordAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID providerId,
            @Valid @RequestBody ProviderAvailabilityRequest availability,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                providers.recordAvailability(TenantScope.companyId(jwt), providerId, availability),
                TraceIdFilter.traceId(httpRequest)
        );
    }
}
