package com.neepo.backend.service;

import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.TelemetryPolicyDecisionJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.care.telemetry.TelemetryPolicyDecision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TelemetryPolicyPersistenceService {
    private final TripRepository trips;
    private final TelemetryPolicyDecisionJdbcRepository policies;

    public TelemetryPolicyPersistenceService(
            TripRepository trips,
            TelemetryPolicyDecisionJdbcRepository policies
    ) {
        this.trips = Objects.requireNonNull(trips, "Trip repository is required");
        this.policies = Objects.requireNonNull(policies, "Telemetry policy repository is required");
    }

    @Transactional
    public boolean persist(UUID companyId, UUID traceId, TelemetryPolicyDecision decision) {
        Objects.requireNonNull(companyId, "Company id is required");
        Objects.requireNonNull(traceId, "Trace id is required");
        Objects.requireNonNull(decision, "Telemetry policy decision is required");

        Trip trip = trips.findByIdAndCompanyId(decision.tripId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found"));
        if (!trip.acceptsDisruption()) {
            throw new ResourceConflictException(
                    "TRIP_NOT_ACTIVE", "Telemetry policy can only be decided for an active trip"
            );
        }

        var existing = policies.findByEventId(decision.eventId());
        if (existing.isPresent()) {
            return exactReplay(existing.get(), decision);
        }
        if (!policies.insertIfAbsent(decision)) {
            TelemetryPolicyDecision concurrent = policies.findByEventId(decision.eventId())
                    .orElseThrow(() -> new ResourceConflictException(
                            "TELEMETRY_POLICY_STATE_INCOMPLETE",
                            "Telemetry policy event exists without a readable decision"
                    ));
            return exactReplay(concurrent, decision);
        }
        policies.insertAudit(traceId, decision);
        return false;
    }

    private static boolean exactReplay(
            TelemetryPolicyDecision existing,
            TelemetryPolicyDecision requested
    ) {
        if (!existing.equals(requested)) {
            throw new ResourceConflictException(
                    "TELEMETRY_POLICY_EVENT_REUSED",
                    "Telemetry policy event id has already been used with another decision"
            );
        }
        return true;
    }
}
