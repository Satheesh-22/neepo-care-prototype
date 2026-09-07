package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.DriverVehicleAssignmentRequest;
import com.neepo.backend.dto.DriverVehicleAssignmentResponse;
import com.neepo.backend.service.AssignmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<List<DriverVehicleAssignmentResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        return ApiResponse.success(
                assignmentService.listAssignments(companyId),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DriverVehicleAssignmentResponse> assign(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DriverVehicleAssignmentRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                assignmentService.assign(companyId, actorUserId, traceId, request),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @org.springframework.web.bind.annotation.RequestMapping(value = "/{assignmentId}/release", method = {org.springframework.web.bind.annotation.RequestMethod.POST, org.springframework.web.bind.annotation.RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<DriverVehicleAssignmentResponse> release(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID assignmentId,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                assignmentService.release(companyId, assignmentId, actorUserId, traceId),
                TraceIdFilter.traceId(httpRequest)
        );
    }
}
