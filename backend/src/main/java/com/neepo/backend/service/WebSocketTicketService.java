package com.neepo.backend.service;

import com.neepo.backend.dto.WebSocketTicketResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketTicketService {
    private static final Duration TICKET_LIFETIME = Duration.ofSeconds(30);
    private final Map<UUID, Ticket> tickets = new ConcurrentHashMap<>();
    private final Clock clock;

    public WebSocketTicketService(Clock clock) {
        this.clock = clock;
    }

    public synchronized WebSocketTicketResponse issue(Jwt jwt) {
        Instant now = clock.instant();
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now)
                || entry.getValue().jwt().getSubject().equals(jwt.getSubject()));
        Instant expiresAt = now.plus(TICKET_LIFETIME);
        UUID value = UUID.randomUUID();
        tickets.put(value, new Ticket(jwt, expiresAt));
        return new WebSocketTicketResponse(value.toString(), expiresAt);
    }

    public Optional<Jwt> consume(String value) {
        try {
            Ticket ticket = tickets.remove(UUID.fromString(value));
            if (ticket == null || !ticket.expiresAt().isAfter(clock.instant())) {
                return Optional.empty();
            }
            return Optional.of(ticket.jwt());
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private record Ticket(Jwt jwt, Instant expiresAt) {
    }
}
