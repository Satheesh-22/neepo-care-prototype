package com.neepo.backend.service;

import com.neepo.backend.config.TenantScope;
import com.neepo.backend.entity.Trip;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DriverRepository;
import com.neepo.backend.repository.TripRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TripReadAccessService {
    private final TripRepository trips;
    private final DriverRepository drivers;

    public TripReadAccessService(TripRepository trips, DriverRepository drivers) {
        this.trips = Objects.requireNonNull(trips, "Trip repository is required");
        this.drivers = Objects.requireNonNull(drivers, "Driver repository is required");
    }

    public void requireReadAccess(Jwt jwt, UUID tripId) {
        Objects.requireNonNull(jwt, "JWT is required");
        Objects.requireNonNull(tripId, "Trip id is required");
        UUID companyId = TenantScope.companyId(jwt);
        Trip trip = trips.findByIdAndCompanyId(tripId, companyId).orElseThrow(this::notFound);
        if (!TenantScope.isDriver(jwt)) {
            return;
        }
        UUID driverId = drivers.findByUserIdAndCompanyId(TenantScope.userId(jwt), companyId)
                .map(driver -> driver.getId())
                .orElseThrow(this::notFound);
        if (!trip.getDriverId().equals(driverId)) {
            throw notFound();
        }
    }

    private ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found");
    }
}
