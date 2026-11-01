package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.FleetAnalyticsSummaryResponse;
import com.neepo.backend.service.FleetAnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final FleetAnalyticsService analyticsService;

    public AnalyticsController(FleetAnalyticsService analyticsService) {
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService is required");
    }

    @GetMapping("/fleet")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<FleetAnalyticsSummaryResponse> getFleetAnalytics(
            @RequestParam(required = false, defaultValue = "30d") String timeframe,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        String traceId = TraceIdFilter.traceId(request);

        FleetAnalyticsSummaryResponse analytics = analyticsService.getFleetAnalytics(companyId, timeframe);
        return ApiResponse.success(analytics, traceId);
    }
}
