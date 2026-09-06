package com.neepo.backend.repository;

import com.neepo.backend.dto.TripContextSnapshotRecord;
import com.neepo.contracts.NetworkState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TripContextSnapshotJdbcRepository {
    private static final String FIND_BY_ID_SQL = """
            SELECT context.id, context.trip_id, context.snapshot_sequence, context.captured_at,
                   context.minimum_confidence, context.has_expired_required_data,
                   context.network_state, context.context_payload::text AS context_payload,
                   context.context_hash
            FROM trip_context_snapshots context
            JOIN trips t ON t.id = context.trip_id
            WHERE context.id = ? AND context.trip_id = ? AND t.company_id = ?
            """;
    private static final String FIND_LATEST_SQL = """
            SELECT context.id, context.trip_id, context.snapshot_sequence, context.captured_at,
                   context.minimum_confidence, context.has_expired_required_data,
                   context.network_state, context.context_payload::text AS context_payload,
                   context.context_hash
            FROM trip_context_snapshots context
            JOIN trips t ON t.id = context.trip_id
            WHERE context.trip_id = ? AND t.company_id = ?
            ORDER BY context.snapshot_sequence DESC, context.id
            LIMIT 1
            """;
    private static final String LOCK_TRIP_SQL = """
            SELECT id
            FROM trips
            WHERE id = ? AND company_id = ?
            FOR UPDATE
            """;
    private static final String NEXT_SEQUENCE_SQL = """
            SELECT COALESCE(MAX(snapshot_sequence), 0) + 1
            FROM trip_context_snapshots
            WHERE trip_id = ?
            """;
    private static final String INSERT_SQL = """
            INSERT INTO trip_context_snapshots (
                id, trip_id, snapshot_sequence, captured_at, minimum_confidence,
                has_expired_required_data, network_state, context_payload, context_hash
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
            """;

    private final JdbcTemplate jdbc;

    public TripContextSnapshotJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean lockTrip(UUID companyId, UUID tripId) {
        return !jdbc.query(LOCK_TRIP_SQL, (resultSet, rowNumber) ->
                resultSet.getObject("id", UUID.class), tripId, companyId).isEmpty();
    }

    public Optional<TripContextSnapshotRecord> findById(
            UUID companyId,
            UUID tripId,
            UUID snapshotId
    ) {
        return jdbc.query(FIND_BY_ID_SQL, (resultSet, rowNumber) -> mapSnapshot(resultSet),
                snapshotId, tripId, companyId).stream().findFirst();
    }

    public Optional<TripContextSnapshotRecord> findLatest(UUID companyId, UUID tripId) {
        return jdbc.query(FIND_LATEST_SQL, (resultSet, rowNumber) -> mapSnapshot(resultSet),
                tripId, companyId).stream().findFirst();
    }

    private static TripContextSnapshotRecord mapSnapshot(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new TripContextSnapshotRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("trip_id", UUID.class),
                resultSet.getLong("snapshot_sequence"),
                resultSet.getObject("captured_at", OffsetDateTime.class).toInstant(),
                resultSet.getBigDecimal("minimum_confidence"),
                resultSet.getBoolean("has_expired_required_data"),
                NetworkState.valueOf(resultSet.getString("network_state")),
                resultSet.getString("context_payload"),
                resultSet.getString("context_hash")
        );
    }

    public long nextSequence(UUID tripId) {
        Long sequence = jdbc.queryForObject(NEXT_SEQUENCE_SQL, Long.class, tripId);
        if (sequence == null || sequence <= 0) {
            throw new IllegalStateException("Could not allocate a context snapshot sequence");
        }
        return sequence;
    }

    public void insert(TripContextSnapshotRecord snapshot) {
        int inserted = jdbc.update(
                INSERT_SQL,
                snapshot.id(),
                snapshot.tripId(),
                snapshot.snapshotSequence(),
                Timestamp.from(snapshot.capturedAt()),
                snapshot.minimumConfidence(),
                snapshot.hasExpiredRequiredData(),
                snapshot.networkState().name(),
                snapshot.contextPayload(),
                snapshot.contextHash().toLowerCase(java.util.Locale.ROOT)
        );
        if (inserted != 1) {
            throw new IllegalStateException("Context snapshot was not persisted");
        }
    }
}
