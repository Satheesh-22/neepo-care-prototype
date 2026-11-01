package com.neepo.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FleetAnalyticsSummaryResponse(
        UUID companyId,
        String timeframe,
        Instant generatedAt,
        int totalTripsMonitored,
        int activeDisruptionsCount,
        int resolvedDisruptionsCount,
        double meanTimeToRecoveryMinutes,
        double driverComplianceRate,
        double estimatedCostAvoidanceUsd,
        double savedTransitHours,
        double spoilageAvoidanceRate,
        List<DisruptionRootCauseDto> topDisruptionRootCauses,
        List<DisruptionTrendPointDto> disruptionTrend,
        List<RecoveryStrategyStatDto> recoveryStrategyBreakdown
) {}
