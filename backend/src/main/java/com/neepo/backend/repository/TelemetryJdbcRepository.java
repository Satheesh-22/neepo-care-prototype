package com.neepo.backend.repository;

import com.neepo.backend.dto.TelemetryObservationRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

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
}
