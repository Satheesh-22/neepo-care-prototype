package com.neepo.backend.repository;

import com.neepo.backend.dto.BenchmarkResultRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.UUID;

@Repository
public class BenchmarkResultJdbcRepository {
    private static final String INSERT_SQL = """
            INSERT INTO benchmark_results (
                id, scenario_code, strategy_code, dataset_version, successful_recovery,
                invalid_recommendations, recovery_time_seconds, rerouting_operations,
                telemetry_transmissions, estimated_bandwidth_bytes, decision_latency_ms,
                stale_data_decisions, measured_at, configuration
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (id) DO NOTHING
            """;
    private static final String AUDIT_SQL = """
            INSERT INTO audit_events (
                trace_id, event_type, aggregate_type, aggregate_id, occurred_at, details
            ) VALUES (?, 'BENCHMARK_RESULT_RECORDED', 'BENCHMARK_RESULT', ?, ?, ?::jsonb)
            """;

    private final JdbcTemplate jdbc;

    public BenchmarkResultJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean insertIfAbsent(BenchmarkResultRecord result) {
        return jdbc.update(
                INSERT_SQL,
                result.id(), result.scenarioCode(), result.strategyCode(), result.datasetVersion(),
                result.successfulRecovery(), result.invalidRecommendations(), result.recoveryTimeSeconds(),
                result.reroutingOperations(), result.telemetryTransmissions(),
                result.estimatedBandwidthBytes(), result.decisionLatencyMs(), result.staleDataDecisions(),
                Timestamp.from(result.measuredAt()), result.configurationJson()
        ) == 1;
    }

    public void insertAudit(UUID traceId, BenchmarkResultRecord result) {
        int inserted = jdbc.update(
                AUDIT_SQL, traceId, result.id(), Timestamp.from(result.measuredAt()), result.configurationJson()
        );
        if (inserted != 1) {
            throw new IllegalStateException("Benchmark result audit event was not persisted");
        }
    }
}
