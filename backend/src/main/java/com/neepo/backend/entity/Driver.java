package com.neepo.backend.entity;

import com.neepo.backend.domain.DriverStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "drivers")
public class Driver {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;
    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private DriverStatus status;
    @Column(name = "max_daily_driving_minutes", nullable = false)
    private int maximumDailyDrivingMinutes;
    @Column(name = "remaining_driving_minutes", nullable = false)
    private int remainingDrivingMinutes;
    @Column(name = "last_rest_completed_at")
    private Instant lastRestCompletedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version @Column(nullable = false)
    private long version;

    protected Driver() {
    }

    public static Driver create(UUID companyId, UUID userId, String licenseNumber, int maximumDailyDrivingMinutes) {
        Driver driver = new Driver();
        driver.companyId = companyId;
        driver.userId = userId;
        driver.licenseNumber = licenseNumber;
        driver.status = DriverStatus.AVAILABLE;
        driver.maximumDailyDrivingMinutes = maximumDailyDrivingMinutes;
        driver.remainingDrivingMinutes = maximumDailyDrivingMinutes;
        return driver;
    }

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getUserId() { return userId; }
    public String getLicenseNumber() { return licenseNumber; }
    public DriverStatus getStatus() { return status; }
    public int getMaximumDailyDrivingMinutes() { return maximumDailyDrivingMinutes; }
    public int getRemainingDrivingMinutes() { return remainingDrivingMinutes; }
    public Instant getLastRestCompletedAt() { return lastRestCompletedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isAvailable() { return status == DriverStatus.AVAILABLE; }
    public void markAssigned() { status = DriverStatus.ASSIGNED; }
    public void markDriving() { status = DriverStatus.DRIVING; }
    public void markAvailable() { status = DriverStatus.AVAILABLE; }
}
