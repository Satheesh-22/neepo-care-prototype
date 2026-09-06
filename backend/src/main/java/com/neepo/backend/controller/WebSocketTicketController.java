package com.neepo.backend.controller;

import com.neepo.backend.api.ApiResponse;
import com.neepo.backend.config.TraceIdFilter;
import com.neepo.backend.dto.WebSocketTicketResponse;
import com.neepo.backend.service.WebSocketTicketService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class WebSocketTicketController {
    private final WebSocketTicketService tickets;

    public WebSocketTicketController(WebSocketTicketService tickets) {
        this.tickets = tickets;
    }

    @PostMapping("/ticket")
    public ApiResponse<WebSocketTicketResponse> issue(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request
    ) {
        return ApiResponse.success(tickets.issue(jwt), TraceIdFilter.traceId(request));
    }
}
