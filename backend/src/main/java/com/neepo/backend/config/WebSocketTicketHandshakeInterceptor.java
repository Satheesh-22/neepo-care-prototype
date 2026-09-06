package com.neepo.backend.config;

import com.neepo.backend.service.TripReadAccessService;
import com.neepo.backend.service.WebSocketTicketService;
import com.neepo.backend.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.UUID;

@Component
public class WebSocketTicketHandshakeInterceptor implements HandshakeInterceptor {
    private final WebSocketTicketService tickets;
    private final TripReadAccessService tripAccess;

    public WebSocketTicketHandshakeInterceptor(WebSocketTicketService tickets, TripReadAccessService tripAccess) {
        this.tickets = tickets;
        this.tripAccess = tripAccess;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        MultiValueMap<String, String> query = UriComponentsBuilder.fromUri(request.getURI())
                .build().getQueryParams();
        String ticket = query.getFirst("ticket");
        String tripValue = query.getFirst("tripId");
        if (ticket == null || tripValue == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        var jwt = tickets.consume(ticket);
        if (jwt.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            UUID tripId = UUID.fromString(tripValue);
            tripAccess.requireReadAccess(jwt.get(), tripId);
            attributes.put("tripId", tripId);
            attributes.put("userId", TenantScope.userId(jwt.get()));
            return true;
        } catch (IllegalArgumentException | ResourceNotFoundException exception) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
        // No post-handshake state is retained; tickets are consumed before upgrade.
    }
}
