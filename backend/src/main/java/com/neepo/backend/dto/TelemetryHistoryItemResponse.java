package com.neepo.backend.dto;

import com.neepo.backend.domain.NetworkState;
import com.neepo.backend.domain.ObservationStatus;
import com.neepo.backend.domain.TelemetrySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelemetryHistoryItemResponse(
        UUID eventId,
        Instant recordedAt,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal engineTemperatureCelsius,
        BigDecimal cargoTemperatureCelsius,
        BigDecimal speedKph,
        BigDecimal confidence,
        ObservationStatus observationStatus,
        TelemetrySource source,
        NetworkState networkState
) {}
