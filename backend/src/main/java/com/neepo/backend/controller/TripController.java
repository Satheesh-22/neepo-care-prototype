package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.AssignTripRequest;
import com.neepo.backend.dto.CreateTripRequest;
import com.neepo.backend.dto.TelemetryHistoryItemResponse;
import com.neepo.backend.dto.TripResponse;
import com.neepo.backend.service.TelemetryService;
import com.neepo.backend.service.TripService;
import com.neepo.backend.service.TripReadAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {
    private final TripService trips;
    private final TripReadAccessService readAccess;
    private final TelemetryService telemetry;

    public TripController(TripService trips, TripReadAccessService readAccess, TelemetryService telemetry) {
        this.trips = trips;
        this.readAccess = readAccess;
        this.telemetry = telemetry;
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TripResponse> create(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody CreateTripRequest request,
                                             HttpServletRequest httpRequest) {
        return ApiResponse.success(trips.create(TenantScope.companyId(jwt), request),
                TraceIdFilter.traceId(httpRequest));
    }

    @GetMapping("/{tripId}")
    public ApiResponse<TripResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tripId,
                                          HttpServletRequest httpRequest) {
        readAccess.requireReadAccess(jwt, tripId);
        return ApiResponse.success(trips.get(TenantScope.companyId(jwt), tripId),
                TraceIdFilter.traceId(httpRequest));
    }

    @GetMapping("/{tripId}/telemetry/history")
    public ApiResponse<List<TelemetryHistoryItemResponse>> getTelemetryHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID tripId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, defaultValue = "500") int limit,
            HttpServletRequest httpRequest) {
        readAccess.requireReadAccess(jwt, tripId);
        return ApiResponse.success(telemetry.getHistory(tripId, from, to, limit),
                TraceIdFilter.traceId(httpRequest));
    }

    @PostMapping("/{tripId}/assign")
    public ApiResponse<TripResponse> assign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tripId,
                                             @Valid @RequestBody AssignTripRequest request,
                                             HttpServletRequest httpRequest) {
        return ApiResponse.success(trips.assign(TenantScope.companyId(jwt), tripId, request),
                TraceIdFilter.traceId(httpRequest));
    }

    @PostMapping("/{tripId}/start")
    public ApiResponse<TripResponse> start(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID tripId,
                                            HttpServletRequest httpRequest) {
        return ApiResponse.success(trips.start(TenantScope.companyId(jwt), tripId),
                TraceIdFilter.traceId(httpRequest));
    }
}

