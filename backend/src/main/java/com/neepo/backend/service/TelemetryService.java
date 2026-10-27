package com.neepo.backend.service;

import com.neepo.backend.dto.TelemetryHistoryItemResponse;
import com.neepo.backend.dto.TelemetryAcceptedResponse;
import com.neepo.backend.dto.TelemetryObservationRequest;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.TelemetryJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TelemetryService {
    private final TripRepository trips;
    private final TelemetryJdbcRepository telemetry;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public TelemetryService(TripRepository trips, TelemetryJdbcRepository telemetry, Clock clock,
                            ApplicationEventPublisher events) {
        this.trips = trips;
        this.telemetry = telemetry;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public TelemetryAcceptedResponse ingest(UUID companyId, TelemetryObservationRequest observation) {
        Trip trip = trips.findByIdAndCompanyId(observation.tripId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found"));
        if (!trip.getVehicleId().equals(observation.vehicleId())) {
            throw new ResourceConflictException("TELEMETRY_VEHICLE_MISMATCH",
                    "Telemetry vehicle does not match the trip assignment");
        }
        boolean inserted = telemetry.insertIfAbsent(observation);
        var acceptedAt = clock.instant();
        if (inserted) {
            events.publishEvent(new TelemetryUpdatedEvent(
                    UUID.randomUUID(), companyId, observation, acceptedAt
            ));
        }
        return new TelemetryAcceptedResponse(observation.eventId(), !inserted, acceptedAt);
    }

    public List<TelemetryHistoryItemResponse> getHistory(UUID tripId, Instant from, Instant to, int limit) {
        return telemetry.findHistory(tripId, from, to, limit);
    }
}

