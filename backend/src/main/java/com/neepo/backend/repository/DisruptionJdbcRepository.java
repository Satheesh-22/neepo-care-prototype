package com.neepo.backend.repository;

import com.neepo.backend.dto.CreateDisruptionRequest;
import com.neepo.backend.dto.ProcessedEventRecord;
import com.neepo.contracts.DisruptionEvent;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
public class DisruptionJdbcRepository {
    private static final String FIND_DISRUPTION_SQL = """
            SELECT d.id, d.event_id, d.trip_id, d.context_snapshot_id, d.disruption_type,
                   d.severity, d.detected_at, d.active, d.explanation_codes, d.evidence::text AS evidence
            FROM disruption_events d
            JOIN trips t ON t.id = d.trip_id
            WHERE d.event_id = ? AND t.company_id = ?
            """;
    private static final String FIND_ACTIVE_DISRUPTION_SQL = """
            SELECT d.id, d.event_id, d.trip_id, d.context_snapshot_id, d.disruption_type,
                   d.severity, d.detected_at, d.active, d.explanation_codes, d.evidence::text AS evidence
            FROM disruption_events d
            JOIN trips t ON t.id = d.trip_id
            WHERE d.trip_id = ? AND t.company_id = ? AND d.active = TRUE
            ORDER BY d.detected_at DESC, d.id
            LIMIT 1
            """;

    private static final String CONTEXT_EXISTS_SQL = """
            SELECT COUNT(*)
            FROM trip_context_snapshots context
            JOIN trips t ON t.id = context.trip_id
            WHERE context.id = ? AND context.trip_id = ? AND t.company_id = ?
            """;

    private static final String INSERT_DISRUPTION_SQL = """
            INSERT INTO disruption_events (
                event_id, trip_id, context_snapshot_id, disruption_type, severity,
                detected_at, explanation_codes, evidence
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (event_id) DO NOTHING
            """;

    private static final String FIND_PROCESSED_EVENT_SQL = """
            SELECT event_type, payload_hash, result_reference_id
            FROM processed_events
            WHERE event_id = ?
            """;

    private static final String INSERT_PROCESSED_EVENT_SQL = """
            INSERT INTO processed_events (
                event_id, event_type, aggregate_type, aggregate_id,
                payload_hash, processed_at, result_reference_id
            ) VALUES (?, 'DISRUPTION_REPORTED', 'TRIP', ?, ?, ?, ?)
            """;

    private static final TypeReference<Map<String, Object>> EVIDENCE_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public DisruptionJdbcRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public boolean contextBelongsToTrip(UUID companyId, UUID tripId, UUID contextSnapshotId) {
        Integer count = jdbc.queryForObject(
                CONTEXT_EXISTS_SQL,
                Integer.class,
                contextSnapshotId,
                tripId,
                companyId
        );
        return count != null && count == 1;
    }

    public Optional<ProcessedEventRecord> findProcessedEvent(UUID eventId) {
        return jdbc.query(
                FIND_PROCESSED_EVENT_SQL,
                (resultSet, rowNumber) -> new ProcessedEventRecord(
                        resultSet.getString("event_type"),
                        resultSet.getString("payload_hash"),
                        resultSet.getObject("result_reference_id", UUID.class)
                ),
                eventId
        ).stream().findFirst();
    }

    public Optional<DisruptionEvent> findByEventId(UUID companyId, UUID eventId) {
        return jdbc.query(FIND_DISRUPTION_SQL, (resultSet, rowNumber) -> mapDisruption(resultSet),
                eventId, companyId).stream().findFirst();
    }

    public Optional<DisruptionEvent> findActiveByTripId(UUID companyId, UUID tripId) {
        return jdbc.query(FIND_ACTIVE_DISRUPTION_SQL, (resultSet, rowNumber) -> mapDisruption(resultSet),
                tripId, companyId).stream().findFirst();
    }

    private DisruptionEvent mapDisruption(ResultSet resultSet) throws SQLException {
        return new DisruptionEvent(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("event_id", UUID.class),
                resultSet.getObject("trip_id", UUID.class),
                resultSet.getObject("context_snapshot_id", UUID.class),
                DisruptionType.valueOf(resultSet.getString("disruption_type")),
                DisruptionSeverity.valueOf(resultSet.getString("severity")),
                resultSet.getObject("detected_at", OffsetDateTime.class).toInstant(),
                resultSet.getBoolean("active"),
                strings(resultSet.getArray("explanation_codes")),
                evidence(resultSet.getString("evidence"))
        );
    }

    public boolean insertIfAbsent(CreateDisruptionRequest disruption) {
        String evidenceJson = writeEvidence(disruption.evidence());
        int inserted = jdbc.update(connection -> {
            var statement = connection.prepareStatement(INSERT_DISRUPTION_SQL);
            statement.setObject(1, disruption.eventId());
            statement.setObject(2, disruption.tripId());
            statement.setObject(3, disruption.contextSnapshotId());
            statement.setString(4, disruption.type().name());
            statement.setString(5, disruption.severity().name());
            statement.setTimestamp(6, Timestamp.from(disruption.detectedAt()));
            statement.setArray(7, connection.createArrayOf("text", disruption.explanationCodes().toArray()));
            statement.setString(8, evidenceJson);
            return statement;
        });
        return inserted == 1;
    }

    public void recordProcessedEvent(
            UUID eventId,
            UUID tripId,
            String payloadHash,
            Instant processedAt,
            UUID disruptionId
    ) {
        jdbc.update(
                INSERT_PROCESSED_EVENT_SQL,
                eventId,
                tripId,
                payloadHash,
                Timestamp.from(processedAt),
                disruptionId
        );
    }

    private Map<String, Object> evidence(String json) throws SQLException {
        try {
            return objectMapper.readValue(json, EVIDENCE_TYPE);
        } catch (JacksonException exception) {
            throw new SQLException("Could not deserialize disruption evidence", exception);
        }
    }

    private String writeEvidence(Map<String, Object> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Disruption evidence cannot be serialized", exception);
        }
    }

    private static List<String> strings(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        try {
            Object values = array.getArray();
            return values instanceof Object[] entries
                    ? Arrays.stream(entries).map(Object::toString).toList()
                    : List.of();
        } finally {
            array.free();
        }
    }
}
