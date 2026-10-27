package com.neepo.backend.repository;

import com.neepo.backend.domain.NetworkState;
import com.neepo.backend.domain.ObservationStatus;
import com.neepo.backend.domain.TelemetrySource;
import com.neepo.backend.dto.TelemetryHistoryItemResponse;
import com.neepo.backend.dto.TelemetryObservationRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class TelemetryJdbcRepository {
    private static final String INSERT_SQL = """
            INSERT INTO telemetry_observations (
                event_id, trip_id, vehicle_id, recorded_at, received_at, expires_at, location,
                engine_temperature_c, cargo_temperature_c, speed_kph, confidence,
                observation_status, source, network_state
            ) VALUES (?, ?, ?, ?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                      ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private final JdbcTemplate jdbc;

    public TelemetryJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean insertIfAbsent(TelemetryObservationRequest observation) {
        int inserted = jdbc.update(connection -> {
            var statement = connection.prepareStatement(INSERT_SQL);
            statement.setObject(1, observation.eventId());
            statement.setObject(2, observation.tripId());
            statement.setObject(3, observation.vehicleId());
            statement.setTimestamp(4, Timestamp.from(observation.recordedAt()));
            statement.setTimestamp(5, Timestamp.from(observation.receivedAt()));
            statement.setTimestamp(6, Timestamp.from(observation.expiresAt()));
            statement.setBigDecimal(7, observation.location().longitude());
            statement.setBigDecimal(8, observation.location().latitude());
            statement.setBigDecimal(9, observation.engineTemperatureCelsius());
            statement.setBigDecimal(10, observation.cargoTemperatureCelsius());
            statement.setBigDecimal(11, observation.speedKph());
            statement.setBigDecimal(12, observation.confidence());
            statement.setString(13, observation.observationStatus().name());
            statement.setString(14, observation.source().name());
            statement.setString(15, observation.networkState().name());
            return statement;
        });
        return inserted == 1;
    }

    public List<TelemetryHistoryItemResponse> findHistory(UUID tripId, Instant from, Instant to, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT event_id, recorded_at,
                       ST_Y(location::geometry) AS latitude,
                       ST_X(location::geometry) AS longitude,
                       engine_temperature_c, cargo_temperature_c, speed_kph, confidence,
                       observation_status, source, network_state
                FROM telemetry_observations
                WHERE trip_id = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(tripId);

        if (from != null) {
            sql.append(" AND recorded_at >= ?");
            params.add(Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" AND recorded_at <= ?");
            params.add(Timestamp.from(to));
        }

        int effectiveLimit = Math.max(1, Math.min(limit <= 0 ? 500 : limit, 2000));
        sql.append(" ORDER BY recorded_at ASC LIMIT ?");
        params.add(effectiveLimit);

        return jdbc.query(connection -> {
            var statement = connection.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            return statement;
        }, (rs, rowNum) -> {
            Timestamp recordedTs = rs.getTimestamp("recorded_at");
            Instant recordedAt = recordedTs != null ? recordedTs.toInstant() : null;
            return new TelemetryHistoryItemResponse(
                    rs.getObject("event_id", UUID.class),
                    recordedAt,
                    rs.getBigDecimal("latitude"),
                    rs.getBigDecimal("longitude"),
                    rs.getBigDecimal("engine_temperature_c"),
                    rs.getBigDecimal("cargo_temperature_c"),
                    rs.getBigDecimal("speed_kph"),
                    rs.getBigDecimal("confidence"),
                    ObservationStatus.valueOf(rs.getString("observation_status")),
                    TelemetrySource.valueOf(rs.getString("source")),
                    NetworkState.valueOf(rs.getString("network_state"))
            );
        });
    }
}
