package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.service.RecoveryQueryService;
import com.neepo.backend.service.TripReadAccessService;
import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.RecoveryPlan;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}")
public class RecoveryQueryController {
    private final RecoveryQueryService recovery;
    private final TripReadAccessService readAccess;

    public RecoveryQueryController(RecoveryQueryService recovery, TripReadAccessService readAccess) {
        this.recovery = recovery;
        this.readAccess = readAccess;
    }

    @GetMapping("/recovery-plans")
    public ApiResponse<List<RecoveryPlan>> listPlans(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID tripId,
            HttpServletRequest request
    ) {
        readAccess.requireReadAccess(jwt, tripId);
        return ApiResponse.success(
                recovery.listPlans(TenantScope.companyId(jwt), tripId),
                TraceIdFilter.traceId(request)
        );
    }

    @GetMapping("/decision-explanation")
    public ApiResponse<DecisionExplanation> latestExplanation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID tripId,
            HttpServletRequest request
    ) {
        readAccess.requireReadAccess(jwt, tripId);
        return ApiResponse.success(
                recovery.latestExplanation(TenantScope.companyId(jwt), tripId),
                TraceIdFilter.traceId(request)
        );
    }
}
