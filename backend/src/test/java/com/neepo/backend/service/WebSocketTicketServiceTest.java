package com.neepo.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketTicketServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-24T13:00:00Z");

    @Test
    void ticketCanBeConsumedOnlyOnce() {
        WebSocketTicketService tickets = new WebSocketTicketService(Clock.fixed(NOW, ZoneOffset.UTC));
        Jwt jwt = jwt();
        var issued = tickets.issue(jwt);

        assertEquals(jwt, tickets.consume(issued.ticket()).orElseThrow());
        assertTrue(tickets.consume(issued.ticket()).isEmpty());
    }

    @Test
    void expiredTicketCannotBeConsumed() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW, NOW.plusSeconds(31));
        WebSocketTicketService tickets = new WebSocketTicketService(clock);

        var issued = tickets.issue(jwt());

        assertTrue(tickets.consume(issued.ticket()).isEmpty());
    }

    @Test
    void issuingAnotherTicketInvalidatesTheUsersPreviousTicket() {
        WebSocketTicketService tickets = new WebSocketTicketService(Clock.fixed(NOW, ZoneOffset.UTC));
        Jwt jwt = jwt();
        var first = tickets.issue(jwt);
        var replacement = tickets.issue(jwt);

        assertTrue(tickets.consume(first.ticket()).isEmpty());
        assertEquals(jwt, tickets.consume(replacement.ticket()).orElseThrow());
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(UUID.randomUUID().toString())
                .claim("companyId", UUID.randomUUID().toString())
                .claim("role", "DRIVER")
                .issuedAt(NOW)
                .expiresAt(NOW.plusSeconds(3_600))
                .build();
    }
}
