package com.neepo.contracts;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelemetryObservation(
        UUID eventId,
        UUID tripId,
        UUID vehicleId,
        Instant recordedAt,
        Instant receivedAt,
        Instant expiresAt,
        GeoPoint location,
        BigDecimal engineTemperatureCelsius,
        BigDecimal cargoTemperatureCelsius,
        BigDecimal speedKph,
        double confidence,
        ObservationStatus observationStatus,
        TelemetrySource source,
        NetworkState networkState
) {
}
