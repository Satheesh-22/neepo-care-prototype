package com.neepo.backend.repository;

import com.neepo.backend.domain.ProviderAvailabilityStatus;
import com.neepo.backend.domain.ProviderType;
import com.neepo.backend.dto.GeoPointResponse;
import com.neepo.backend.dto.ProviderAvailabilityRequest;
import com.neepo.backend.dto.ProviderAvailabilityResponse;
import com.neepo.backend.dto.ProviderResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProviderJdbcRepository {
    private static final String PROVIDER_SELECT = """
            SELECT p.id,
                   p.name,
                   p.provider_type,
                   ST_Y(p.location::geometry) AS latitude,
                   ST_X(p.location::geometry) AS longitude,
                   p.service_radius_km,
                   p.reliability_score,
                   p.active,
                   ARRAY(
                       SELECT pc.capability_code
                       FROM provider_capabilities pc
                       WHERE pc.provider_id = p.id
                       ORDER BY pc.capability_code
                   ) AS capabilities,
                   availability.status AS availability_status,
                   availability.observed_at,
                   availability.expires_at,
                   availability.estimated_arrival_minutes,
                   availability.confidence,
                   availability.required_resource_available
            FROM service_providers p
            LEFT JOIN LATERAL (
                SELECT pa.status,
                       pa.observed_at,
                       pa.expires_at,
                       pa.estimated_arrival_minutes,
                       pa.confidence,
                       pa.required_resource_available
                FROM provider_availability pa
                WHERE pa.provider_id = p.id
                ORDER BY pa.observed_at DESC, pa.created_at DESC, pa.id DESC
                LIMIT 1
            ) availability ON TRUE
            """;

    private static final String LIST_SQL = PROVIDER_SELECT + """
            WHERE (p.company_id = ? OR p.company_id IS NULL)
              AND p.active = TRUE
            ORDER BY p.name, p.id
            LIMIT ? OFFSET ?
            """;

    private static final String FIND_SQL = PROVIDER_SELECT + """
            WHERE (p.company_id = ? OR p.company_id IS NULL)
              AND p.id = ?
              AND p.active = TRUE
            """;

    private static final String INSERT_AVAILABILITY_SQL = """
            INSERT INTO provider_availability (
                provider_id, status, observed_at, expires_at, estimated_arrival_minutes,
                confidence, required_resource_available
            )
            SELECT p.id, ?, ?, ?, ?, ?, ?
            FROM service_providers p
            WHERE p.id = ?
              AND (p.company_id = ? OR p.company_id IS NULL)
              AND p.active = TRUE
            """;

    private static final RowMapper<ProviderResponse> PROVIDER_ROW_MAPPER = (resultSet, rowNumber) -> {
        ProviderAvailabilityResponse availability = null;
        String availabilityStatus = resultSet.getString("availability_status");
        if (availabilityStatus != null) {
            availability = new ProviderAvailabilityResponse(
                    ProviderAvailabilityStatus.valueOf(availabilityStatus),
                    resultSet.getObject("observed_at", OffsetDateTime.class).toInstant(),
                    resultSet.getObject("expires_at", OffsetDateTime.class).toInstant(),
                    resultSet.getObject("estimated_arrival_minutes", Integer.class),
                    resultSet.getBigDecimal("confidence"),
                    resultSet.getBoolean("required_resource_available")
            );
        }

        return new ProviderResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                ProviderType.valueOf(resultSet.getString("provider_type")),
                new GeoPointResponse(
                        resultSet.getBigDecimal("latitude"),
                        resultSet.getBigDecimal("longitude")
                ),
                resultSet.getBigDecimal("service_radius_km"),
                resultSet.getBigDecimal("reliability_score"),
                resultSet.getBoolean("active"),
                capabilities(resultSet.getArray("capabilities")),
                availability
        );
    };

    private final JdbcTemplate jdbc;

    public ProviderJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProviderResponse> findAccessible(UUID companyId, int page, int size) {
        return jdbc.query(LIST_SQL, PROVIDER_ROW_MAPPER, companyId, size, (long) page * size);
    }

    public Optional<ProviderResponse> findAccessibleById(UUID companyId, UUID providerId) {
        return jdbc.query(FIND_SQL, PROVIDER_ROW_MAPPER, companyId, providerId).stream().findFirst();
    }

    public boolean appendAvailability(UUID companyId, UUID providerId, ProviderAvailabilityRequest availability) {
        int inserted = jdbc.update(connection -> {
            var statement = connection.prepareStatement(INSERT_AVAILABILITY_SQL);
            statement.setString(1, availability.status().name());
            statement.setTimestamp(2, Timestamp.from(availability.observedAt()));
            statement.setTimestamp(3, Timestamp.from(availability.expiresAt()));
            if (availability.estimatedArrivalMinutes() == null) {
                statement.setNull(4, Types.INTEGER);
            } else {
                statement.setInt(4, availability.estimatedArrivalMinutes());
            }
            statement.setBigDecimal(5, availability.confidence());
            statement.setBoolean(6, availability.requiredResourceAvailable());
            statement.setObject(7, providerId);
            statement.setObject(8, companyId);
            return statement;
        });
        return inserted == 1;
    }

    private static List<String> capabilities(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        try {
            Object values = array.getArray();
            if (!(values instanceof Object[] entries)) {
                return List.of();
            }
            return Arrays.stream(entries).map(Object::toString).toList();
        } finally {
            array.free();
        }
    }
}
