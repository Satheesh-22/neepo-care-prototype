package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.AssignTripRequest;
import com.neepo.backend.dto.CreateTripRequest;
import com.neepo.backend.dto.TripResponse;
import com.neepo.backend.service.TripService;
import com.neepo.backend.service.TripReadAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {
    private final TripService trips;
    private final TripReadAccessService readAccess;
    public TripController(TripService trips, TripReadAccessService readAccess) {
        this.trips = trips;
        this.readAccess = readAccess;
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
