package com.neepo.backend.repository;

import com.neepo.backend.domain.NetworkState;
import com.neepo.backend.domain.ObservationStatus;
import com.neepo.backend.domain.TelemetrySource;
import com.neepo.backend.dto.GeoPointRequest;
import com.neepo.backend.dto.TelemetryObservationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryJdbcRepositoryTest {
    @Test
    void bindsInstantsAsPostgresCompatibleTimestamps() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(any(String.class))).thenReturn(statement);
        when(jdbc.update(any(PreparedStatementCreator.class))).thenAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            creator.createPreparedStatement(connection);
            return 1;
        });

        Instant recordedAt = Instant.parse("2026-08-24T16:30:00Z");
        Instant receivedAt = recordedAt.plusSeconds(1);
        Instant expiresAt = recordedAt.plusSeconds(300);
        var observation = new TelemetryObservationRequest(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                recordedAt, receivedAt, expiresAt,
                new GeoPointRequest(new BigDecimal("11.42"), new BigDecimal("77.69")),
                new BigDecimal("96.5"), new BigDecimal("5.2"), new BigDecimal("42"),
                new BigDecimal("0.98"), ObservationStatus.FRESH,
                TelemetrySource.SIMULATOR, NetworkState.ONLINE
        );

        assertThat(new TelemetryJdbcRepository(jdbc).insertIfAbsent(observation)).isTrue();
        verify(statement).setTimestamp(4, Timestamp.from(recordedAt));
        verify(statement).setTimestamp(5, Timestamp.from(receivedAt));
        verify(statement).setTimestamp(6, Timestamp.from(expiresAt));
    }
}
