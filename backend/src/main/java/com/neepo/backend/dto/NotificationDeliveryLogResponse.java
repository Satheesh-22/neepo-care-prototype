package com.neepo.backend.dto;

import com.neepo.backend.domain.NotificationDeliveryStatus;
import com.neepo.backend.entity.NotificationDeliveryLog;
import com.neepo.contracts.DisruptionSeverity;

import java.time.Instant;
import java.util.UUID;

public record NotificationDeliveryLogResponse(
        UUID id,
        UUID channelId,
        UUID companyId,
        UUID disruptionEventId,
        String disruptionType,
        DisruptionSeverity severity,
        NotificationDeliveryStatus status,
        String signature,
        String payload,
        String errorMessage,
        Instant deliveredAt
) {
    public static NotificationDeliveryLogResponse from(NotificationDeliveryLog log) {
        return new NotificationDeliveryLogResponse(
                log.getId(),
                log.getChannelId(),
                log.getCompanyId(),
                log.getDisruptionEventId(),
                log.getDisruptionType(),
                log.getSeverity(),
                log.getStatus(),
                log.getSignature(),
                log.getPayload(),
                log.getErrorMessage(),
                log.getDeliveredAt()
        );
    }
}
