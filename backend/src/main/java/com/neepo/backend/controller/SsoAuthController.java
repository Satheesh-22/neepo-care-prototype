package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.SsoDiscoveryResponse;
import com.neepo.backend.dto.SsoLoginRequest;
import com.neepo.backend.dto.SsoLoginResponse;
import com.neepo.backend.service.SsoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/sso")
public class SsoAuthController {

    private final SsoService ssoService;

    public SsoAuthController(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    @GetMapping("/discover")
    public ApiResponse<SsoDiscoveryResponse> discover(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String domain,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(ssoService.discover(email, domain), TraceIdFilter.traceId(httpRequest));
    }

    @PostMapping("/callback")
    public ApiResponse<SsoLoginResponse> callback(
            @Valid @RequestBody SsoLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(ssoService.processSsoCallback(request, httpRequest), TraceIdFilter.traceId(httpRequest));
    }
}

