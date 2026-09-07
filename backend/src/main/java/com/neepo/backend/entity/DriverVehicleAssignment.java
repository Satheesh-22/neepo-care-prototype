package com.neepo.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "driver_vehicle_assignments")
public class DriverVehicleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "assigned_by_user_id")
    private UUID assignedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    protected DriverVehicleAssignment() {
    }

    public static DriverVehicleAssignment create(UUID companyId, UUID driverId, UUID vehicleId,
                                                Instant startTime, Instant endTime, String notes,
                                                UUID assignedByUserId) {
        DriverVehicleAssignment assignment = new DriverVehicleAssignment();
        assignment.companyId = companyId;
        assignment.driverId = driverId;
        assignment.vehicleId = vehicleId;
        assignment.startTime = startTime != null ? startTime : Instant.now();
        assignment.endTime = endTime;
        assignment.notes = notes;
        assignment.status = "ACTIVE";
        assignment.assignedByUserId = assignedByUserId;
        return assignment;
    }

    public void release(Instant at) {
        this.status = "RELEASED";
        this.releasedAt = at != null ? at : Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getDriverId() { return driverId; }
    public UUID getVehicleId() { return vehicleId; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public String getNotes() { return notes; }
    public String getStatus() { return status; }
    public UUID getAssignedByUserId() { return assignedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReleasedAt() { return releasedAt; }
}
