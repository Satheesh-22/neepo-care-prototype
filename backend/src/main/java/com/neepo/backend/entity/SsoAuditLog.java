package com.neepo.backend.entity;

import com.neepo.backend.domain.IdpType;
import com.neepo.backend.domain.SsoEventType;
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
@Table(name = "sso_audit_logs")
public class SsoAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "user_email", nullable = false, length = 128)
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "idp_type", nullable = false, length = 32)
    private IdpType idpType = IdpType.OIDC;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private SsoEventType eventType;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "details", columnDefinition = "jsonb")
    private String details = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SsoAuditLog() {
    }

    public SsoAuditLog(UUID companyId, String userEmail, IdpType idpType, SsoEventType eventType) {
        this.companyId = companyId;
        this.userEmail = userEmail;
        this.idpType = idpType;
        this.eventType = eventType;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public IdpType getIdpType() {
        return idpType;
    }

    public void setIdpType(IdpType idpType) {
        this.idpType = idpType;
    }

    public SsoEventType getEventType() {
        return eventType;
    }

    public void setEventType(SsoEventType eventType) {
        this.eventType = eventType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
