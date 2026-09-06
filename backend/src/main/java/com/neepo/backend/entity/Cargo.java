package com.neepo.backend.entity;

import com.neepo.backend.domain.CargoType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cargo")
public class Cargo {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "reference_code", nullable = false, length = 50)
    private String referenceCode;
    @Enumerated(EnumType.STRING) @Column(name = "cargo_type", nullable = false, length = 30)
    private CargoType cargoType;
    @Column(nullable = false, length = 300)
    private String description;
    @Column(name = "weight_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal weightKg;
    @Column(name = "minimum_temperature_c", precision = 6, scale = 2)
    private BigDecimal minimumTemperatureCelsius;
    @Column(name = "maximum_temperature_c", precision = 6, scale = 2)
    private BigDecimal maximumTemperatureCelsius;
    @Column(nullable = false)
    private boolean hazardous;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version @Column(nullable = false)
    private long version;

    protected Cargo() {
    }

    public static Cargo create(UUID companyId, String referenceCode, CargoType cargoType, String description,
                               BigDecimal weightKg, BigDecimal minimumTemperatureCelsius,
                               BigDecimal maximumTemperatureCelsius, boolean hazardous) {
        Cargo cargo = new Cargo();
        cargo.companyId = companyId;
        cargo.referenceCode = referenceCode;
        cargo.cargoType = cargoType;
        cargo.description = description;
        cargo.weightKg = weightKg;
        cargo.minimumTemperatureCelsius = minimumTemperatureCelsius;
        cargo.maximumTemperatureCelsius = maximumTemperatureCelsius;
        cargo.hazardous = hazardous;
        return cargo;
    }

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getReferenceCode() { return referenceCode; }
    public CargoType getCargoType() { return cargoType; }
    public String getDescription() { return description; }
    public BigDecimal getWeightKg() { return weightKg; }
    public BigDecimal getMinimumTemperatureCelsius() { return minimumTemperatureCelsius; }
    public BigDecimal getMaximumTemperatureCelsius() { return maximumTemperatureCelsius; }
    public boolean isHazardous() { return hazardous; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
