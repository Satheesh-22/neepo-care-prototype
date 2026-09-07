package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.CreateDriverDetailedRequest;
import com.neepo.backend.dto.CreateDriverRequest;
import com.neepo.backend.dto.DriverDetailedResponse;
import com.neepo.backend.dto.DriverResponse;
import com.neepo.backend.dto.UpdateDriverRequest;
import com.neepo.backend.dto.UserInvitationResponse;
import com.neepo.backend.service.DriverService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {
    private final DriverService drivers;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public DriverController(DriverService drivers, ObjectMapper objectMapper, Validator validator) {
        this.drivers = drivers;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<?> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody JsonNode jsonNode,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));

        if (jsonNode.hasNonNull("userId")) {
            try {
                CreateDriverRequest request = objectMapper.treeToValue(jsonNode, CreateDriverRequest.class);
                var violations = validator.validate(request);
                if (!violations.isEmpty()) {
                    throw new jakarta.validation.ConstraintViolationException(violations);
                }
                return ApiResponse.success(
                        drivers.create(companyId, request),
                        TraceIdFilter.traceId(httpRequest)
                );
            } catch (tools.jackson.core.JacksonException e) {
                throw new IllegalArgumentException("Invalid driver request payload: " + e.getMessage(), e);
            }
        } else {
            try {
                CreateDriverDetailedRequest request = objectMapper.treeToValue(jsonNode, CreateDriverDetailedRequest.class);
                var violations = validator.validate(request);
                if (!violations.isEmpty()) {
                    throw new jakarta.validation.ConstraintViolationException(violations);
                }
                return ApiResponse.success(
                        drivers.createDetailed(companyId, actorUserId, traceId, request),
                        TraceIdFilter.traceId(httpRequest)
                );
            } catch (tools.jackson.core.JacksonException e) {
                throw new IllegalArgumentException("Invalid driver request payload: " + e.getMessage(), e);
            }
        }
    }

    @PostMapping("/simple")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DriverResponse> createSimple(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDriverRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                drivers.create(TenantScope.companyId(jwt), request),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping
    public ApiResponse<List<DriverDetailedResponse>> list(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                drivers.listDrivers(TenantScope.companyId(jwt)),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping("/{driverId}")
    public ApiResponse<DriverDetailedResponse> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID driverId,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                drivers.getDetailed(TenantScope.companyId(jwt), driverId),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping("/me")
    public ApiResponse<DriverDetailedResponse> getMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID userId = TenantScope.userId(jwt);
        return ApiResponse.success(
                drivers.getByUserId(companyId, userId),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PatchMapping("/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DriverDetailedResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID driverId,
            @Valid @RequestBody UpdateDriverRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                drivers.updateDriver(companyId, driverId, actorUserId, traceId, request),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/{driverId}/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserInvitationResponse> invite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID driverId,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                drivers.inviteDriver(companyId, driverId, actorUserId, traceId),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/{driverId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DriverDetailedResponse> suspend(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID driverId,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                drivers.suspendDriver(companyId, driverId, actorUserId, traceId),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/{driverId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DriverDetailedResponse> reactivate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID driverId,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                drivers.reactivateDriver(companyId, driverId, actorUserId, traceId),
                TraceIdFilter.traceId(httpRequest)
        );
    }
}
