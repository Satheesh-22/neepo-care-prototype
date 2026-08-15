package com.neepo.contracts;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RouteContext(
        UUID routeId,
        RouteStatus status,
        GeoPoint origin,
        GeoPoint destination,
        GeoPoint currentLocation,
        BigDecimal remainingDistanceKm,
        int remainingDurationMinutes,
        List<RouteRestriction> restrictions,
        ObservationMetadata conditionObservation
) {
}
