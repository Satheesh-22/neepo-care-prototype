package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.TelemetryPolicyRequest;
import com.neepo.backend.service.TelemetryPolicyEvaluationService;
import com.neepo.backend.service.TripReadAccessService;
import com.neepo.care.telemetry.TelemetryPolicyDecision;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/telemetry-policy")
public class TelemetryPolicyController {
    private final TelemetryPolicyEvaluationService policies;
    private final TripReadAccessService readAccess;

    public TelemetryPolicyController(
            TelemetryPolicyEvaluationService policies,
            TripReadAccessService readAccess
    ) {
        this.policies = policies;
        this.readAccess = readAccess;
    }

    @PostMapping
    public ApiResponse<TelemetryPolicyDecision> evaluate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID tripId,
            @Valid @RequestBody TelemetryPolicyRequest request,
            HttpServletRequest httpRequest
    ) {
        readAccess.requireReadAccess(jwt, tripId);
        String traceId = TraceIdFilter.traceId(httpRequest);
        return ApiResponse.success(
                policies.evaluate(
                        TenantScope.companyId(jwt), tripId, UUID.fromString(traceId), request
                ),
                traceId
        );
    }
}
