package com.neepo.backend.entity;

import com.neepo.backend.domain.TripStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trips")
public class Trip {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;
    @Column(name = "driver_id", nullable = false)
    private UUID driverId;
    @Column(name = "cargo_id", nullable = false)
    private UUID cargoId;
    @Column(name = "route_id", nullable = false)
    private UUID routeId;
    @Column(name = "reference_code", nullable = false, length = 50)
    private String referenceCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TripStatus status;
    @Column(name = "delivery_deadline", nullable = false)
    private Instant deliveryDeadline;
    @Column(name = "maximum_waiting_minutes", nullable = false)
    private int maximumWaitingMinutes;
    @Column(name = "accepted_eta_threshold_minutes", nullable = false)
    private int acceptedEtaThresholdMinutes;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version @Column(nullable = false)
    private long version;

    protected Trip() {
    }

    public static Trip create(UUID companyId, UUID vehicleId, UUID driverId, UUID cargoId, UUID routeId,
                              String referenceCode, Instant deliveryDeadline, int maximumWaitingMinutes,
                              int acceptedEtaThresholdMinutes) {
        Trip trip = new Trip();
        trip.companyId = companyId;
        trip.vehicleId = vehicleId;
        trip.driverId = driverId;
        trip.cargoId = cargoId;
        trip.routeId = routeId;
        trip.referenceCode = referenceCode;
        trip.status = TripStatus.CREATED;
        trip.deliveryDeadline = deliveryDeadline;
        trip.maximumWaitingMinutes = maximumWaitingMinutes;
        trip.acceptedEtaThresholdMinutes = acceptedEtaThresholdMinutes;
        return trip;
    }

    public void assign(UUID vehicleId, UUID driverId) {
        if (status != TripStatus.CREATED && status != TripStatus.ASSIGNED) {
            throw new IllegalStateException("Only created or assigned trips can be assigned");
        }
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.status = TripStatus.ASSIGNED;
    }

    public void start(Instant at) {
        if (status != TripStatus.ASSIGNED) {
            throw new IllegalStateException("Trip must be assigned before it can start");
        }
        status = TripStatus.IN_PROGRESS;
        startedAt = at;
    }

    public boolean acceptsDisruption() {
        return status == TripStatus.IN_PROGRESS
                || status == TripStatus.DISRUPTED
                || status == TripStatus.RECOVERY_IN_PROGRESS;
    }

    public void markDisrupted() {
        if (!acceptsDisruption()) {
            throw new IllegalStateException("Only an active trip can be disrupted");
        }
        status = TripStatus.DISRUPTED;
    }

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getVehicleId() { return vehicleId; }
    public UUID getDriverId() { return driverId; }
    public UUID getCargoId() { return cargoId; }
    public UUID getRouteId() { return routeId; }
    public String getReferenceCode() { return referenceCode; }
    public TripStatus getStatus() { return status; }
    public Instant getDeliveryDeadline() { return deliveryDeadline; }
    public int getMaximumWaitingMinutes() { return maximumWaitingMinutes; }
    public int getAcceptedEtaThresholdMinutes() { return acceptedEtaThresholdMinutes; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
