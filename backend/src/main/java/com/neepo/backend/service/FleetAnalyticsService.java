package com.neepo.backend.service;

import com.neepo.backend.dto.DisruptionRootCauseDto;
import com.neepo.backend.dto.DisruptionTrendPointDto;
import com.neepo.backend.dto.FleetAnalyticsSummaryResponse;
import com.neepo.backend.dto.RecoveryStrategyStatDto;
import com.neepo.backend.entity.FleetAnalyticsSnapshot;
import com.neepo.backend.repository.FleetAnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FleetAnalyticsService {

    private static final Set<String> VALID_TIMEFRAMES = Set.of("7d", "30d", "90d");

    private static final TypeReference<List<DisruptionRootCauseDto>> ROOT_CAUSES_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<DisruptionTrendPointDto>> TRENDS_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<RecoveryStrategyStatDto>> STRATEGIES_TYPE = new TypeReference<>() {};

    private final FleetAnalyticsRepository repository;
    private final ObjectMapper objectMapper;

    public FleetAnalyticsService(FleetAnalyticsRepository repository, ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository, "repository is required");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper is required");
    }

    public FleetAnalyticsSummaryResponse getFleetAnalytics(UUID companyId, String timeframeParam) {
        Objects.requireNonNull(companyId, "companyId is required");

        String timeframe = (timeframeParam == null || timeframeParam.isBlank())
                ? "30d"
                : timeframeParam.trim().toLowerCase();

        if (!VALID_TIMEFRAMES.contains(timeframe)) {
            throw new IllegalArgumentException("Timeframe must be one of: 7d, 30d, 90d");
        }

        Optional<FleetAnalyticsSnapshot> snapshotOpt =
                repository.findFirstByCompanyIdAndTimeframeOrderBySnapshotAtDesc(companyId, timeframe);

        if (snapshotOpt.isPresent()) {
            FleetAnalyticsSnapshot snapshot = snapshotOpt.get();
            List<DisruptionRootCauseDto> rootCauses = parseJson(snapshot.getRootCausesJson(), ROOT_CAUSES_TYPE, List.of());
            List<DisruptionTrendPointDto> trends = parseJson(snapshot.getTrendsJson(), TRENDS_TYPE, List.of());
            List<RecoveryStrategyStatDto> strategies = parseJson(snapshot.getStrategiesJson(), STRATEGIES_TYPE, List.of());

            return new FleetAnalyticsSummaryResponse(
                    snapshot.getCompanyId(),
                    snapshot.getTimeframe(),
                    snapshot.getSnapshotAt(),
                    snapshot.getTotalTripsMonitored(),
                    snapshot.getActiveDisruptionsCount(),
                    snapshot.getResolvedDisruptionsCount(),
                    snapshot.getMeanTimeToRecoveryMinutes(),
                    snapshot.getDriverComplianceRate(),
                    snapshot.getEstimatedCostAvoidanceUsd(),
                    snapshot.getSavedTransitHours(),
                    snapshot.getSpoilageAvoidanceRate(),
                    rootCauses,
                    trends,
                    strategies
            );
        }

        // Clean fallback baseline for new organizations without persisted historical records
        return new FleetAnalyticsSummaryResponse(
                companyId,
                timeframe,
                Instant.now(),
                0,
                0,
                0,
                0.0,
                100.0,
                0.0,
                0.0,
                100.0,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            String unescaped = json.trim();
            if (unescaped.startsWith("\"") && unescaped.endsWith("\"")) {
                unescaped = objectMapper.readValue(unescaped, String.class);
            }
            return objectMapper.readValue(unescaped, typeRef);
        } catch (Exception e) {
            return fallback;
        }
    }
}
