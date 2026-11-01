package com.neepo.backend.dto;

public record RecoveryStrategyStatDto(
        String strategyName,
        int usageCount,
        double successRate,
        double averageCostAvoidanceUsd
) {}
