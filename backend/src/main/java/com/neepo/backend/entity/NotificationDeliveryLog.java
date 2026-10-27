package com.neepo.backend.entity;

import com.neepo.backend.domain.NotificationDeliveryStatus;
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
@Table(name = "notification_delivery_logs")
public class NotificationDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "disruption_event_id")
    private UUID disruptionEventId;

    @Column(name = "disruption_type", nullable = false, length = 60)
    private String disruptionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisruptionSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationDeliveryStatus status;

    @Column(length = 100)
    private String signature;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "delivered_at", nullable = false, updatable = false)
    private Instant deliveredAt;

    protected NotificationDeliveryLog() {
    }

    public static NotificationDeliveryLog create(
            UUID channelId,
            UUID companyId,
            UUID disruptionEventId,
            String disruptionType,
            DisruptionSeverity severity,
            NotificationDeliveryStatus status,
            String signature,
            String payload,
            String errorMessage
    ) {
        NotificationDeliveryLog log = new NotificationDeliveryLog();
        log.channelId = channelId;
        log.companyId = companyId;
        log.disruptionEventId = disruptionEventId;
        log.disruptionType = disruptionType;
        log.severity = severity;
        log.status = status;
        log.signature = signature;
        log.payload = payload;
        log.errorMessage = errorMessage;
        log.deliveredAt = Instant.now();
        return log;
    }

    @PrePersist
    void onCreate() {
        if (deliveredAt == null) {
            deliveredAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getDisruptionEventId() {
        return disruptionEventId;
    }

    public String getDisruptionType() {
        return disruptionType;
    }

    public DisruptionSeverity getSeverity() {
        return severity;
    }

    public NotificationDeliveryStatus getStatus() {
        return status;
    }

    public String getSignature() {
        return signature;
    }

    public String getPayload() {
        return payload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }
}
