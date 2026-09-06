package com.neepo.backend.dto;

import com.neepo.backend.domain.NetworkState;
import com.neepo.backend.domain.ObservationStatus;
import com.neepo.backend.domain.TelemetrySource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelemetryObservationRequest(
        @NotNull UUID eventId,
        @NotNull UUID tripId,
        @NotNull UUID vehicleId,
        @NotNull Instant recordedAt,
        @NotNull Instant receivedAt,
        @NotNull Instant expiresAt,
        @NotNull @Valid GeoPointRequest location,
        @DecimalMin("-50") @DecimalMax("250") BigDecimal engineTemperatureCelsius,
        @DecimalMin("-100") @DecimalMax("100") BigDecimal cargoTemperatureCelsius,
        @DecimalMin("0") BigDecimal speedKph,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal confidence,
        @NotNull ObservationStatus observationStatus,
        @NotNull TelemetrySource source,
        @NotNull NetworkState networkState
) {
    @AssertTrue(message = "Telemetry timestamps must be ordered recordedAt <= receivedAt and recordedAt < expiresAt")
    public boolean isTimestampOrderValid() {
        return recordedAt == null || receivedAt == null || expiresAt == null
                || (!receivedAt.isBefore(recordedAt) && expiresAt.isAfter(recordedAt));
    }
}
