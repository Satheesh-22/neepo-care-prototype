package com.neepo.backend.entity;

import com.neepo.backend.domain.UserRole;
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
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 16)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "last_active_at")
    private Instant lastActiveAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected User() {
    }

    private User(String name, String email, String phone, UserRole role, String passwordHash) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.passwordHash = passwordHash;
        this.status = "ACTIVE";
        this.enabled = true;
    }

    public static User create(String name, String email, String phone, UserRole role) {
        return new User(name, email, phone, role, "unusable");
    }

    public static User create(String name, String email, String phone, UserRole role, String passwordHash) {
        return new User(name, email, phone, role, passwordHash);
    }

    public static User createForCompany(UUID companyId, String name, String email, String phone,
                                        UserRole role, String passwordHash) {
        User user = new User(name, email, phone, role, passwordHash);
        user.companyId = companyId;
        return user;
    }

    public static User createInvited(UUID companyId, String name, String email, String phone, UserRole role) {
        User user = new User(name, email, phone, role, "{INVITED}" + UUID.randomUUID());
        user.companyId = companyId;
        user.status = "INVITED";
        user.enabled = false;
        return user;
    }

    public void suspend() {
        this.status = "SUSPENDED";
        this.enabled = false;
    }

    public void reactivate() {
        this.status = "ACTIVE";
        this.enabled = true;
    }

    public void activateWithPassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
        this.status = "ACTIVE";
        this.enabled = true;
    }

    public void updateDetails(String name, String phone, UserRole role) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone.trim();
        }
        if (role != null) {
            this.role = role;
        }
    }

    public void recordActive(Instant at) {
        this.lastActiveAt = at;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = enabled ? "ACTIVE" : "SUSPENDED";
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public UserRole getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEnabled() {
        return enabled && "ACTIVE".equals(status);
    }

    public String getStatus() {
        return status;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
