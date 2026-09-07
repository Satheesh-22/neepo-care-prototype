package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.api.PageResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.domain.UserRole;
import com.neepo.backend.dto.AcceptInvitationRequest;
import com.neepo.backend.dto.AuditEventResponse;
import com.neepo.backend.dto.CreateUserInvitationRequest;
import com.neepo.backend.dto.CreateUserRequest;
import com.neepo.backend.dto.UpdateUserRequest;
import com.neepo.backend.dto.UserInvitationResponse;
import com.neepo.backend.dto.UserResponse;
import com.neepo.backend.service.AuditService;
import com.neepo.backend.service.UserInvitationService;
import com.neepo.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserInvitationService invitationService;
    private final AuditService auditService;

    public UserController(UserService userService, UserInvitationService invitationService, AuditService auditService) {
        this.userService = userService;
        this.invitationService = invitationService;
        this.auditService = auditService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> createUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUserRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        String roleStr = jwt.getClaimAsString("role");
        UserRole actorRole = roleStr != null ? UserRole.valueOf(roleStr) : UserRole.DISPATCHER;
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));

        return ApiResponse.success(
                userService.createUser(companyId, actorUserId, actorRole, traceId, request),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserInvitationResponse> inviteUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUserInvitationRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        String roleStr = jwt.getClaimAsString("role");
        UserRole actorRole = roleStr != null ? UserRole.valueOf(roleStr) : UserRole.DISPATCHER;
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));

        return ApiResponse.success(
                invitationService.inviteUser(companyId, actorUserId, actorRole, traceId, request),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/invitations/accept")
    public ApiResponse<UserResponse> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        return ApiResponse.success(
                invitationService.acceptInvitation(request, traceId),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/invitations/{invitationId}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvitation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID invitationId,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        invitationService.revokeInvitation(companyId, invitationId, actorUserId, traceId);
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));

        return ApiResponse.success(
                userService.updateUser(companyId, userId, actorUserId, traceId, request),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/{userId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> suspendUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));

        return ApiResponse.success(
                userService.suspendUser(companyId, userId, actorUserId, traceId),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @PostMapping("/{userId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> reactivateUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));

        return ApiResponse.success(
                userService.reactivateUser(companyId, userId, actorUserId, traceId),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> getUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be zero or greater") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must not exceed 100") int size,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                userService.getUsers(TenantScope.companyId(jwt), page, size),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                userService.getUserById(TenantScope.companyId(jwt), userId),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping("/by-email")
    public ApiResponse<UserResponse> getUserByEmail(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @Email(message = "Email must be valid") String email,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                userService.getUserByEmail(TenantScope.companyId(jwt), email),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AuditEventResponse>> getUserAudit(
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(
                auditService.getEventsForAggregateType("USER", 50),
                TraceIdFilter.traceId(httpRequest)
        );
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        UUID actorUserId = TenantScope.userId(jwt);
        UUID traceId = UUID.fromString(TraceIdFilter.traceId(httpRequest));
        userService.deleteUser(companyId, userId, actorUserId, traceId);
    }
}
