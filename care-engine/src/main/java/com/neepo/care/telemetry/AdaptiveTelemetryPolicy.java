package com.neepo.care.telemetry;

public record AdaptiveTelemetryPolicy(
        int normalIntervalSeconds,
        int nearDecisionIntervalSeconds,
        int routeDeviationIntervalSeconds,
        int emergencyIntervalSeconds,
        int safelyParkedIntervalSeconds
) {
    public AdaptiveTelemetryPolicy {
        if (normalIntervalSeconds <= 0
                || nearDecisionIntervalSeconds <= 0
                || routeDeviationIntervalSeconds <= 0
                || emergencyIntervalSeconds <= 0
                || safelyParkedIntervalSeconds <= 0) {
            throw new IllegalArgumentException("Online telemetry intervals must be positive");
        }
    }

    public static AdaptiveTelemetryPolicy demoDefaults() {
        return new AdaptiveTelemetryPolicy(60, 15, 5, 3, 120);
    }
}
