package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TenantScope;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.CreateNotificationChannelRequest;
import com.neepo.backend.dto.NotificationChannelResponse;
import com.neepo.backend.dto.NotificationDeliveryLogResponse;
import com.neepo.backend.dto.NotificationTestResultResponse;
import com.neepo.backend.service.NotificationAlertService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationAlertService notificationAlertService;

    public NotificationController(NotificationAlertService notificationAlertService) {
        this.notificationAlertService = notificationAlertService;
    }

    @GetMapping("/channels")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<List<NotificationChannelResponse>> getChannels(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        List<NotificationChannelResponse> channels = notificationAlertService.listChannels(companyId);
        return ApiResponse.success(channels, TraceIdFilter.traceId(httpRequest));
    }

    @PostMapping("/channels")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NotificationChannelResponse> createChannel(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateNotificationChannelRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        NotificationChannelResponse response = notificationAlertService.createChannel(companyId, request);
        return ApiResponse.success(response, TraceIdFilter.traceId(httpRequest));
    }

    @DeleteMapping("/channels/{channelId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChannel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID channelId
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        notificationAlertService.deleteChannel(companyId, channelId);
    }

    @PostMapping("/channels/{channelId}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<NotificationTestResultResponse> testChannel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID channelId,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        NotificationTestResultResponse result = notificationAlertService.testChannel(companyId, channelId);
        return ApiResponse.success(result, TraceIdFilter.traceId(httpRequest));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ApiResponse<List<NotificationDeliveryLogResponse>> getDeliveryLogs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest httpRequest
    ) {
        UUID companyId = TenantScope.companyId(jwt);
        List<NotificationDeliveryLogResponse> logs = notificationAlertService.getDeliveryLogs(companyId, limit);
        return ApiResponse.success(logs, TraceIdFilter.traceId(httpRequest));
    }
}
