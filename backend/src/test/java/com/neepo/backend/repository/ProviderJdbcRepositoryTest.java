package com.neepo.backend.repository;

import com.neepo.backend.domain.ProviderAvailabilityStatus;
import com.neepo.backend.dto.ProviderAvailabilityRequest;
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

class ProviderJdbcRepositoryTest {
    @Test
    void bindsAvailabilityInstantsAsPostgresCompatibleTimestamps() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(connection.prepareStatement(any(String.class))).thenReturn(statement);
        when(jdbc.update(any(PreparedStatementCreator.class))).thenAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            creator.createPreparedStatement(connection);
            return 1;
        });

        Instant observedAt = Instant.parse("2026-08-24T17:00:00Z");
        Instant expiresAt = observedAt.plusSeconds(1800);
        var availability = new ProviderAvailabilityRequest(
                ProviderAvailabilityStatus.AVAILABLE,
                observedAt,
                expiresAt,
                20,
                new BigDecimal("0.95"),
                true
        );

        assertThat(new ProviderJdbcRepository(jdbc).appendAvailability(
                UUID.randomUUID(), UUID.randomUUID(), availability
        )).isTrue();
        verify(statement).setTimestamp(2, Timestamp.from(observedAt));
        verify(statement).setTimestamp(3, Timestamp.from(expiresAt));
    }
}
