package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.CreateDisruptionRequest;
import com.neepo.backend.service.DisruptionService;
import com.neepo.contracts.DisruptionEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/disruptions")
public class DisruptionController {
    private final DisruptionService disruptions;

    public DisruptionController(DisruptionService disruptions) {
        this.disruptions = disruptions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<DisruptionEvent> report(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDisruptionRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                disruptions.report(TenantScope.companyId(jwt), request),
                TraceIdFilter.traceId(httpRequest)
        );
    }
}
