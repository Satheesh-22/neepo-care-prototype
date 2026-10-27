package com.neepo.backend.dto;

import com.neepo.backend.domain.NotificationDeliveryStatus;

import java.util.UUID;

public record NotificationTestResultResponse(
        UUID channelId,
        NotificationDeliveryStatus status,
        String message,
        String signature,
        String payload
) {
}
