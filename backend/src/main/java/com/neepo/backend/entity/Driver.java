package com.neepo.backend.entity;

import com.neepo.backend.domain.DriverStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "drivers")
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @Column(name = "license_category", length = 30)
    private String licenseCategory = "HEAVY_RIGID";

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;

    @Column(name = "base_location", length = 150)
    private String baseLocation;

    @Column(name = "employment_status", length = 30)
    private String employmentStatus = "FULL_TIME";

    @Column(name = "profile_photo_url", length = 255)
    private String profilePhotoUrl;

    @Column(name = "verification_state", length = 30)
    private String verificationState = "VERIFIED";

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
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

    @Version
    @Column(nullable = false)
    private long version;

    protected Driver() {
    }

    public static Driver create(UUID companyId, UUID userId, String licenseNumber, int maximumDailyDrivingMinutes) {
        Driver driver = new Driver();
        driver.companyId = companyId;
        driver.userId = userId;
        driver.licenseNumber = licenseNumber;
        driver.status = DriverStatus.AVAILABLE;
        driver.maximumDailyDrivingMinutes = maximumDailyDrivingMinutes > 0 ? maximumDailyDrivingMinutes : 600;
        driver.remainingDrivingMinutes = driver.maximumDailyDrivingMinutes;
        driver.licenseCategory = "HEAVY_RIGID";
        driver.employmentStatus = "FULL_TIME";
        driver.verificationState = "VERIFIED";
        return driver;
    }

    public static Driver createDetailed(UUID companyId, UUID userId, String employeeId,
                                        String firstName, String lastName, String email, String phone,
                                        String licenseNumber, String licenseCategory, LocalDate licenseExpiryDate,
                                        String emergencyContactName, String emergencyContactPhone,
                                        String baseLocation, String employmentStatus, String profilePhotoUrl,
                                        String verificationState, int maximumDailyDrivingMinutes) {
        Driver driver = new Driver();
        driver.companyId = companyId;
        driver.userId = userId;
        driver.employeeId = employeeId;
        driver.firstName = firstName;
        driver.lastName = lastName;
        driver.email = email;
        driver.phone = phone;
        driver.licenseNumber = licenseNumber;
        driver.licenseCategory = licenseCategory != null ? licenseCategory : "HEAVY_RIGID";
        driver.licenseExpiryDate = licenseExpiryDate;
        driver.emergencyContactName = emergencyContactName;
        driver.emergencyContactPhone = emergencyContactPhone;
        driver.baseLocation = baseLocation;
        driver.employmentStatus = employmentStatus != null ? employmentStatus : "FULL_TIME";
        driver.profilePhotoUrl = profilePhotoUrl;
        driver.verificationState = verificationState != null ? verificationState : "VERIFIED";
        driver.status = DriverStatus.AVAILABLE;
        driver.maximumDailyDrivingMinutes = maximumDailyDrivingMinutes > 0 ? maximumDailyDrivingMinutes : 600;
        driver.remainingDrivingMinutes = driver.maximumDailyDrivingMinutes;
        return driver;
    }

    public void updateProfile(String employeeId, String firstName, String lastName, String phone,
                              String licenseNumber, String licenseCategory, LocalDate licenseExpiryDate,
                              String emergencyContactName, String emergencyContactPhone,
                              String baseLocation, String employmentStatus, String profilePhotoUrl,
                              int maximumDailyDrivingMinutes) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        if (licenseCategory != null) this.licenseCategory = licenseCategory;
        this.licenseExpiryDate = licenseExpiryDate;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.baseLocation = baseLocation;
        if (employmentStatus != null) this.employmentStatus = employmentStatus;
        this.profilePhotoUrl = profilePhotoUrl;
        if (maximumDailyDrivingMinutes > 0) {
            this.maximumDailyDrivingMinutes = maximumDailyDrivingMinutes;
        }
    }

    public void setVerificationState(String verificationState) {
        this.verificationState = verificationState;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public boolean isLicenseExpired(LocalDate onDate) {
        return licenseExpiryDate != null && licenseExpiryDate.isBefore(onDate);
    }

    public boolean isSuspended() {
        return "SUSPENDED".equalsIgnoreCase(verificationState) || "OFF_DUTY".equals(status.name());
    }

    public void suspend() {
        this.verificationState = "SUSPENDED";
        this.status = DriverStatus.OFF_DUTY;
    }

    public void reactivate() {
        this.verificationState = "VERIFIED";
        this.status = DriverStatus.AVAILABLE;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getUserId() { return userId; }
    public String getEmployeeId() { return employeeId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getLicenseNumber() { return licenseNumber; }
    public String getLicenseCategory() { return licenseCategory; }
    public LocalDate getLicenseExpiryDate() { return licenseExpiryDate; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public String getBaseLocation() { return baseLocation; }
    public String getEmploymentStatus() { return employmentStatus; }
    public String getProfilePhotoUrl() { return profilePhotoUrl; }
    public String getVerificationState() { return verificationState; }
    public Instant getLastActiveAt() { return lastActiveAt; }
    public DriverStatus getStatus() { return status; }
    public int getMaximumDailyDrivingMinutes() { return maximumDailyDrivingMinutes; }
    public int getRemainingDrivingMinutes() { return remainingDrivingMinutes; }
    public Instant getLastRestCompletedAt() { return lastRestCompletedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean isAvailable() {
        return status == DriverStatus.AVAILABLE && !"SUSPENDED".equalsIgnoreCase(verificationState);
    }
    public void markAssigned() { status = DriverStatus.ASSIGNED; }
    public void markDriving() { status = DriverStatus.DRIVING; }
    public void markAvailable() { status = DriverStatus.AVAILABLE; }
}
