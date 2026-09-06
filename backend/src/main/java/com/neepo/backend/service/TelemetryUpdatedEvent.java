package com.neepo.backend.service;

import com.neepo.backend.dto.TelemetryObservationRequest;

import java.time.Instant;
import java.util.UUID;

public record TelemetryUpdatedEvent(
        UUID eventId,
        UUID companyId,
        TelemetryObservationRequest observation,
        Instant occurredAt
) {
}
