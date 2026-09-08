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
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_invitations")
public class UserInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    @Column(nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserInvitation() {
    }

    public static UserInvitation create(UUID companyId, UUID userId, String tokenHash, String email,
                                        UserRole role, Instant expiresAt, UUID createdByUserId) {
        UserInvitation invitation = new UserInvitation();
        invitation.companyId = companyId;
        invitation.userId = userId;
        invitation.tokenHash = tokenHash;
        invitation.email = email;
        invitation.role = role;
        invitation.expiresAt = expiresAt;
        invitation.createdByUserId = createdByUserId;
        return invitation;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isAccepted() {
        return acceptedAt != null;
    }

    public void markAccepted(Instant at) {
        this.acceptedAt = at;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
