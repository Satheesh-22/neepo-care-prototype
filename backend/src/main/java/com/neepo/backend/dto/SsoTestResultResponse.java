package com.neepo.backend.dto;

import java.time.Instant;

public record SsoTestResultResponse(
        boolean success,
        String idpType,
        String issuerUrl,
        double latencyMs,
        String message,
        Instant testedAt
) {}
