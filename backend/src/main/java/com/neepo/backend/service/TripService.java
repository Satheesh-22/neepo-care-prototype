package com.neepo.backend.service;

import com.neepo.backend.domain.TripStatus;
import com.neepo.backend.dto.*;
import com.neepo.backend.entity.*;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TripService {
    private final TripRepository trips;
    private final TripAssignmentRepository assignments;
    private final VehicleRepository vehicles;
    private final DriverRepository drivers;
    private final CargoRepository cargo;
    private final RouteReferenceRepository routes;
    private final Clock clock;

    public TripService(TripRepository trips, TripAssignmentRepository assignments, VehicleRepository vehicles,
                       DriverRepository drivers, CargoRepository cargo, RouteReferenceRepository routes, Clock clock) {
        this.trips = trips;
        this.assignments = assignments;
        this.vehicles = vehicles;
        this.drivers = drivers;
        this.cargo = cargo;
        this.routes = routes;
        this.clock = clock;
    }

    @Transactional
    public TripResponse create(UUID companyId, CreateTripRequest request) {
        String reference = request.referenceCode().trim().toUpperCase(Locale.ROOT);
        if (trips.existsByCompanyIdAndReferenceCodeIgnoreCase(companyId, reference)) {
            throw new ResourceConflictException("TRIP_REFERENCE_EXISTS", "Trip reference already exists");
        }
        requireVehicle(companyId, request.vehicleId());
        requireDriver(companyId, request.driverId());
        if (!cargo.existsById(request.cargoId())
                || cargo.findByIdAndCompanyId(request.cargoId(), companyId).isEmpty()) {
            throw new ResourceNotFoundException("CARGO_NOT_FOUND", "Cargo was not found in this company");
        }
        if (!routes.existsByIdAndCompanyId(request.routeId(), companyId)) {
            throw new ResourceNotFoundException("ROUTE_NOT_FOUND", "Route was not found in this company");
        }
        Trip trip = Trip.create(companyId, request.vehicleId(), request.driverId(), request.cargoId(),
                request.routeId(), reference, request.deliveryDeadline(), request.maximumWaitingMinutes(),
                request.acceptedEtaThresholdMinutes());
        return toResponse(trips.save(trip));
    }

    public TripResponse get(UUID companyId, UUID tripId) {
        return toResponse(requireTrip(companyId, tripId));
    }

    @Transactional
    public TripResponse assign(UUID companyId, UUID tripId, AssignTripRequest request) {
        Trip trip = requireTrip(companyId, tripId);
        if (trip.getStatus() != TripStatus.CREATED && trip.getStatus() != TripStatus.ASSIGNED) {
            throw new ResourceConflictException("TRIP_NOT_ASSIGNABLE", "Trip cannot be assigned in its current state");
        }
        Vehicle vehicle = requireVehicle(companyId, request.vehicleId());
        Driver driver = requireDriver(companyId, request.driverId());
        TripAssignment current = assignments.findFirstByTripIdAndReleasedAtIsNull(tripId).orElse(null);
        boolean sameVehicle = current != null && current.getVehicleId().equals(vehicle.getId());
        boolean sameDriver = current != null && current.getDriverId().equals(driver.getId());
        if (!sameVehicle && !vehicle.isAvailable()) {
            throw new ResourceConflictException("VEHICLE_NOT_AVAILABLE", "Vehicle is not available");
        }
        if (!sameDriver && !driver.isAvailable()) {
            throw new ResourceConflictException("DRIVER_NOT_AVAILABLE", "Driver is not available");
        }
        if (current != null) {
            current.release(request.assignedAt());
            if (!sameVehicle) vehicles.findById(current.getVehicleId()).ifPresent(Vehicle::markAvailable);
            if (!sameDriver) drivers.findById(current.getDriverId()).ifPresent(Driver::markAvailable);
        }
        vehicle.markAssigned();
        driver.markAssigned();
        trip.assign(vehicle.getId(), driver.getId());
        assignments.save(TripAssignment.create(tripId, vehicle.getId(), driver.getId(), request.assignedAt()));
        return toResponse(trip);
    }

    @Transactional
    public TripResponse start(UUID companyId, UUID tripId) {
        Trip trip = requireTrip(companyId, tripId);
        if (trip.getStatus() != TripStatus.ASSIGNED) {
            throw new ResourceConflictException("TRIP_NOT_STARTABLE", "Trip must be assigned before it can start");
        }
        Vehicle vehicle = requireVehicle(companyId, trip.getVehicleId());
        Driver driver = requireDriver(companyId, trip.getDriverId());
        Instant now = clock.instant();
        trip.start(now);
        vehicle.markInTransit();
        driver.markDriving();
        return toResponse(trip);
    }

    private Trip requireTrip(UUID companyId, UUID id) {
        return trips.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Trip was not found"));
    }
    private Vehicle requireVehicle(UUID companyId, UUID id) {
        return vehicles.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_NOT_FOUND", "Vehicle was not found"));
    }
    private Driver requireDriver(UUID companyId, UUID id) {
        return drivers.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_NOT_FOUND", "Driver was not found"));
    }
    private TripResponse toResponse(Trip trip) {
        return new TripResponse(trip.getId(), trip.getCompanyId(), trip.getReferenceCode(), trip.getVehicleId(),
                trip.getDriverId(), trip.getCargoId(), trip.getRouteId(), trip.getStatus(), trip.getDeliveryDeadline(),
                trip.getMaximumWaitingMinutes(), trip.getAcceptedEtaThresholdMinutes(), trip.getStartedAt(),
                trip.getCreatedAt(), trip.getUpdatedAt());
    }
}
