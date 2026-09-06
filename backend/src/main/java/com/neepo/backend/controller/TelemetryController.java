package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.TelemetryAcceptedResponse;
import com.neepo.backend.dto.TelemetryObservationRequest;
import com.neepo.backend.service.TelemetryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telemetry")
public class TelemetryController {
    private final TelemetryService telemetry;
    public TelemetryController(TelemetryService telemetry) { this.telemetry = telemetry; }

    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<TelemetryAcceptedResponse> ingest(@AuthenticationPrincipal Jwt jwt,
                                                          @Valid @RequestBody TelemetryObservationRequest request,
                                                          HttpServletRequest httpRequest) {
        return ApiResponse.success(telemetry.ingest(TenantScope.companyId(jwt), request),
                TraceIdFilter.traceId(httpRequest));
    }
}
