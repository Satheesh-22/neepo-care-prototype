package com.neepo.backend.dto;

import com.neepo.backend.domain.NotificationChannelType;
import com.neepo.contracts.DisruptionSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationChannelRequest(
        @NotBlank @Size(min = 2, max = 100)
        String name,

        @NotNull
        NotificationChannelType channelType,

        @NotBlank @Size(min = 5, max = 500)
        String targetEndpoint,

        @Size(min = 8, max = 255)
        String secretKey,

        @NotNull
        DisruptionSeverity minimumSeverity,

        Boolean active
) {
    public boolean isActiveOrTrue() {
        return active == null || active;
    }
}
