package com.neepo.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record TelemetryAcceptedResponse(UUID eventId, boolean duplicate, Instant acceptedAt) {
}
