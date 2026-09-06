package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.InvalidateRecoveryPlanRequest;
import com.neepo.backend.service.RecoveryInvalidationService;
import com.neepo.contracts.RecoveryPlan;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recovery-plans")
public class RecoveryCommandController {
    private final RecoveryInvalidationService invalidation;

    public RecoveryCommandController(RecoveryInvalidationService invalidation) {
        this.invalidation = invalidation;
    }

    @PostMapping("/{planId}/invalidate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<RecoveryPlan> invalidate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID planId,
            @Valid @RequestBody InvalidateRecoveryPlanRequest request,
            HttpServletRequest httpRequest
    ) {
        String traceId = TraceIdFilter.traceId(httpRequest);
        return ApiResponse.success(
                invalidation.invalidate(
                        TenantScope.companyId(jwt), UUID.fromString(traceId), planId, request
                ),
                traceId
        );
    }
}
