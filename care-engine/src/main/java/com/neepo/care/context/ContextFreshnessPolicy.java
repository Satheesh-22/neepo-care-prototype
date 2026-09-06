package com.neepo.care.context;

import java.time.Duration;

public record ContextFreshnessPolicy(Duration agingWindow, double minimumConfirmedConfidence) {
    public static final Duration DEFAULT_AGING_WINDOW = Duration.ofMinutes(2);
    public static final double DEFAULT_MINIMUM_CONFIDENCE = 0.80;

    public ContextFreshnessPolicy {
        if (agingWindow == null || agingWindow.isNegative()) {
            throw new IllegalArgumentException("Aging window must not be negative");
        }
        if (!Double.isFinite(minimumConfirmedConfidence)
                || minimumConfirmedConfidence < 0
                || minimumConfirmedConfidence > 1) {
            throw new IllegalArgumentException("Minimum confidence must be between 0 and 1");
        }
    }

    public static ContextFreshnessPolicy defaults() {
        return new ContextFreshnessPolicy(DEFAULT_AGING_WINDOW, DEFAULT_MINIMUM_CONFIDENCE);
    }
}
