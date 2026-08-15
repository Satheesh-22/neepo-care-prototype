package com.neepo.contracts;

import java.time.Instant;
import java.util.List;

public record TripContext(
        Trip trip,
        VehicleContext vehicle,
        DriverContext driver,
        CargoContext cargo,
        RouteContext route,
        List<ProviderContext> providers,
        List<SafeStopContext> safeStops,
        List<TelemetryObservation> telemetryObservations,
        NetworkState networkState,
        List<ActiveDisruption> activeDisruptions,
        RecoveryPlan currentRecoveryPlan,
        Instant evaluationTime,
        String contextHash
) {
}
