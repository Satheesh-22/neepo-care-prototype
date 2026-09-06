package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.service.RecoveryPlanGenerationService;
import com.neepo.contracts.RecoveryPlan;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/recovery-plans")
public class RecoveryGenerationController {
    private final RecoveryPlanGenerationService generation;

    public RecoveryGenerationController(RecoveryPlanGenerationService generation) {
        this.generation = generation;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<List<RecoveryPlan>> generate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID tripId,
            HttpServletRequest request
    ) {
        String traceId = TraceIdFilter.traceId(request);
        return ApiResponse.success(
                generation.generate(
                        TenantScope.companyId(jwt), tripId, UUID.fromString(traceId)
                ),
                traceId
        );
    }
}
