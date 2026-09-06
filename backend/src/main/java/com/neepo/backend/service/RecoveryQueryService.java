package com.neepo.backend.service;

import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.RecoveryQueryJdbcRepository;
import com.neepo.backend.repository.TripRepository;
import com.neepo.contracts.DecisionExplanation;
import com.neepo.contracts.RecoveryPlan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RecoveryQueryService {
    private final TripRepository trips;
    private final RecoveryQueryJdbcRepository recovery;

    public RecoveryQueryService(TripRepository trips, RecoveryQueryJdbcRepository recovery) {
        this.trips = Objects.requireNonNull(trips, "Trip repository is required");
        this.recovery = Objects.requireNonNull(recovery, "Recovery query repository is required");
    }

    public List<RecoveryPlan> listPlans(UUID companyId, UUID tripId) {
        requireTrip(companyId, tripId);
        return recovery.findPlans(companyId, tripId);
    }

    public DecisionExplanation latestExplanation(UUID companyId, UUID tripId) {
        requireTrip(companyId, tripId);
        return recovery.findLatestExplanation(companyId, tripId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DECISION_EXPLANATION_NOT_FOUND",
                        "No recovery decision explanation was found for this trip"
                ));
    }

    private void requireTrip(UUID companyId, UUID tripId) {
        Objects.requireNonNull(companyId, "Company id is required");
        Objects.requireNonNull(tripId, "Trip id is required");
        if (trips.findByIdAndCompanyId(tripId, companyId).isEmpty()) {
            throw new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found");
        }
    }
}
