package com.neepo.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ComplianceVerificationRequest(
        @NotBlank(message = "Payload is required for verification")
        String payload
) {
}
