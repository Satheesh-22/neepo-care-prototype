package com.neepo.backend.entity;

import com.neepo.backend.domain.VehicleStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "registration_number", nullable = false, length = 30)
    private String registrationNumber;
    @Column(name = "vehicle_type", nullable = false, length = 60)
    private String vehicleType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private VehicleStatus status;
    @Column(name = "height_meters", nullable = false, precision = 6, scale = 2)
    private BigDecimal heightMeters;
    @Column(name = "width_meters", nullable = false, precision = 6, scale = 2)
    private BigDecimal widthMeters;
    @Column(name = "gross_weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossWeightKg;
    @Column(name = "payload_capacity_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal payloadCapacityKg;
    @Column(name = "maximum_engine_temperature_c", nullable = false, precision = 6, scale = 2)
    private BigDecimal maximumEngineTemperatureCelsius;
    @Column(name = "refrigeration_supported", nullable = false)
    private boolean refrigerationSupported;
    @Column(name = "current_engine_health_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal currentEngineHealthPercent;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version @Column(nullable = false)
    private long version;

    protected Vehicle() {
    }

    public static Vehicle create(UUID companyId, String registrationNumber, String vehicleType,
                                 BigDecimal heightMeters, BigDecimal widthMeters, BigDecimal grossWeightKg,
                                 BigDecimal payloadCapacityKg, BigDecimal maximumEngineTemperatureCelsius,
                                 boolean refrigerationSupported) {
        Vehicle vehicle = new Vehicle();
        vehicle.companyId = companyId;
        vehicle.registrationNumber = registrationNumber;
        vehicle.vehicleType = vehicleType;
        vehicle.status = VehicleStatus.AVAILABLE;
        vehicle.heightMeters = heightMeters;
        vehicle.widthMeters = widthMeters;
        vehicle.grossWeightKg = grossWeightKg;
        vehicle.payloadCapacityKg = payloadCapacityKg;
        vehicle.maximumEngineTemperatureCelsius = maximumEngineTemperatureCelsius;
        vehicle.refrigerationSupported = refrigerationSupported;
        vehicle.currentEngineHealthPercent = BigDecimal.valueOf(100);
        return vehicle;
    }

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getVehicleType() { return vehicleType; }
    public VehicleStatus getStatus() { return status; }
    public BigDecimal getHeightMeters() { return heightMeters; }
    public BigDecimal getWidthMeters() { return widthMeters; }
    public BigDecimal getGrossWeightKg() { return grossWeightKg; }
    public BigDecimal getPayloadCapacityKg() { return payloadCapacityKg; }
    public BigDecimal getMaximumEngineTemperatureCelsius() { return maximumEngineTemperatureCelsius; }
    public boolean isRefrigerationSupported() { return refrigerationSupported; }
    public BigDecimal getCurrentEngineHealthPercent() { return currentEngineHealthPercent; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isAvailable() { return status == VehicleStatus.AVAILABLE; }
    public void markAssigned() { status = VehicleStatus.ASSIGNED; }
    public void markInTransit() { status = VehicleStatus.IN_TRANSIT; }
    public void markAvailable() { status = VehicleStatus.AVAILABLE; }
}
