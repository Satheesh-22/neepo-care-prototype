package com.neepo.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trip_assignments")
public class TripAssignment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "trip_id", nullable = false)
    private UUID tripId;
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;
    @Column(name = "driver_id", nullable = false)
    private UUID driverId;
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;
    @Column(name = "released_at")
    private Instant releasedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TripAssignment() {
    }

    public static TripAssignment create(UUID tripId, UUID vehicleId, UUID driverId, Instant assignedAt) {
        TripAssignment assignment = new TripAssignment();
        assignment.tripId = tripId;
        assignment.vehicleId = vehicleId;
        assignment.driverId = driverId;
        assignment.assignedAt = assignedAt;
        return assignment;
    }

    public void release(Instant at) { releasedAt = at; }
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public UUID getVehicleId() { return vehicleId; }
    public UUID getDriverId() { return driverId; }
}
