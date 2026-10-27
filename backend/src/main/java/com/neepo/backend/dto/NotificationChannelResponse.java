package com.neepo.backend.dto;

import com.neepo.backend.domain.NotificationChannelType;
import com.neepo.backend.entity.NotificationChannel;
import com.neepo.contracts.DisruptionSeverity;

import java.time.Instant;
import java.util.UUID;

public record NotificationChannelResponse(
        UUID id,
        UUID companyId,
        String name,
        NotificationChannelType channelType,
        String targetEndpoint,
        String secretKey,
        DisruptionSeverity minimumSeverity,
        boolean active,
        Instant createdAt
) {
    public static NotificationChannelResponse from(NotificationChannel channel) {
        return new NotificationChannelResponse(
                channel.getId(),
                channel.getCompanyId(),
                channel.getName(),
                channel.getChannelType(),
                channel.getTargetEndpoint(),
                maskSecret(channel.getSecretKey()),
                channel.getMinimumSeverity(),
                channel.isActive(),
                channel.getCreatedAt()
        );
    }

    public static String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "********";
        }
        if (secret.length() <= 8) {
            return "********";
        }
        return secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);
    }
}
