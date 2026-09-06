package com.neepo.backend.config;

import com.neepo.backend.service.TripReadAccessService;
import com.neepo.backend.service.WebSocketTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.socket.WebSocketHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketTicketHandshakeInterceptorTest {
    private static final UUID TRIP_ID = UUID.fromString("00000000-0000-4000-8600-000000000001");

    @Test
    void validTicketAndTripAccessAuthorizeHandshake() {
        WebSocketTicketService tickets = mock(WebSocketTicketService.class);
        TripReadAccessService access = mock(TripReadAccessService.class);
        ServerHttpRequest request = request("/ws?tripId=" + TRIP_ID + "&ticket=valid-ticket");
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        Jwt jwt = jwt();
        when(tickets.consume("valid-ticket")).thenReturn(Optional.of(jwt));
        var attributes = new HashMap<String, Object>();

        boolean accepted = new WebSocketTicketHandshakeInterceptor(tickets, access)
                .beforeHandshake(request, response, mock(WebSocketHandler.class), attributes);

        assertTrue(accepted);
        assertEquals(TRIP_ID, attributes.get("tripId"));
        verify(access).requireReadAccess(jwt, TRIP_ID);
    }

    @Test
    void missingTicketRejectsHandshake() {
        WebSocketTicketService tickets = mock(WebSocketTicketService.class);
        TripReadAccessService access = mock(TripReadAccessService.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);

        boolean accepted = new WebSocketTicketHandshakeInterceptor(tickets, access)
                .beforeHandshake(request("/ws?tripId=" + TRIP_ID), response,
                        mock(WebSocketHandler.class), new HashMap<>());

        assertFalse(accepted);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    private ServerHttpRequest request(String uri) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(uri));
        return request;
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("ticket-owner")
                .header("alg", "none")
                .subject("00000000-0000-4000-8100-000000000002")
                .claim("companyId", "00000000-0000-4000-8000-000000000001")
                .claim("role", "DRIVER")
                .issuedAt(Instant.parse("2026-08-24T13:00:00Z"))
                .expiresAt(Instant.parse("2026-08-24T14:00:00Z"))
                .build();
    }
}
