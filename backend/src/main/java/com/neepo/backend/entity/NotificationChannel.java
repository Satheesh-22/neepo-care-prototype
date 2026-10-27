package com.neepo.backend.entity;

import com.neepo.backend.domain.NotificationChannelType;
import com.neepo.contracts.DisruptionSeverity;
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
@Table(name = "notification_channels")
public class NotificationChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 30)
    private NotificationChannelType channelType;

    @Column(name = "target_endpoint", nullable = false, length = 500)
    private String targetEndpoint;

    @Column(name = "secret_key", nullable = false, length = 255)
    private String secretKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "minimum_severity", nullable = false, length = 30)
    private DisruptionSeverity minimumSeverity;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationChannel() {
    }

    public static NotificationChannel create(
            UUID companyId,
            String name,
            NotificationChannelType channelType,
            String targetEndpoint,
            String secretKey,
            DisruptionSeverity minimumSeverity,
            boolean active
    ) {
        NotificationChannel channel = new NotificationChannel();
        channel.companyId = companyId;
        channel.name = name;
        channel.channelType = channelType;
        channel.targetEndpoint = targetEndpoint;
        channel.secretKey = secretKey;
        channel.minimumSeverity = minimumSeverity != null ? minimumSeverity : DisruptionSeverity.LOW;
        channel.active = active;
        channel.createdAt = Instant.now();
        return channel;
    }

    @PrePersist
    void onCreate() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NotificationChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(NotificationChannelType channelType) {
        this.channelType = channelType;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public DisruptionSeverity getMinimumSeverity() {
        return minimumSeverity;
    }

    public void setMinimumSeverity(DisruptionSeverity minimumSeverity) {
        this.minimumSeverity = minimumSeverity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
