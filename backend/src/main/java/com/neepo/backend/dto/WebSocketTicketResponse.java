package com.neepo.backend.dto;

import java.time.Instant;

public record WebSocketTicketResponse(String ticket, Instant expiresAt) {
}
