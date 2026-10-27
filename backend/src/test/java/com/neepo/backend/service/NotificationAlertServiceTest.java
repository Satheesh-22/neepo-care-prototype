package com.neepo.backend.service;

import com.neepo.backend.domain.NotificationChannelType;
import com.neepo.backend.domain.NotificationDeliveryStatus;
import com.neepo.backend.dto.CreateNotificationChannelRequest;
import com.neepo.backend.dto.NotificationChannelResponse;
import com.neepo.backend.dto.NotificationTestResultResponse;
import com.neepo.backend.entity.NotificationChannel;
import com.neepo.backend.entity.NotificationDeliveryLog;
import com.neepo.backend.repository.NotificationChannelRepository;
import com.neepo.backend.repository.NotificationDeliveryLogRepository;
import com.neepo.contracts.DisruptionEvent;
import com.neepo.contracts.DisruptionSeverity;
import com.neepo.contracts.DisruptionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationAlertServiceTest {
    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID CHANNEL_ID = UUID.fromString("20000000-0000-4000-8000-000000000002");
    private static final Instant NOW = Instant.parse("2026-10-27T12:00:00Z");

    private NotificationChannelRepository channels;
    private NotificationDeliveryLogRepository deliveryLogs;
    private WebhookDispatcher webhookDispatcher;
    private NotificationAlertService service;

    @BeforeEach
    void setUp() {
        channels = mock(NotificationChannelRepository.class);
        deliveryLogs = mock(NotificationDeliveryLogRepository.class);
        webhookDispatcher = mock(WebhookDispatcher.class);
        service = new NotificationAlertService(
                channels,
                deliveryLogs,
                webhookDispatcher,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void calculatesDeterministicHmacSha256Hex() {
        String key = "testSecret123";
        String payload = "{\"event\":\"TEST\"}";
        String signature = NotificationAlertService.calculateHmacSha256(key, payload);

        assertNotNull(signature);
        assertEquals(64, signature.length()); // SHA-256 is 32 bytes = 64 hex characters
        assertEquals(signature, NotificationAlertService.calculateHmacSha256(key, payload));
    }

    @Test
    void masksSecretCorrectly() {
        assertEquals("neep...2026", NotificationChannelResponse.maskSecret("neepo_secret_ops_slack_2026"));
        assertEquals("********", NotificationChannelResponse.maskSecret("short"));
        assertEquals("********", NotificationChannelResponse.maskSecret(null));
        assertEquals("********", NotificationChannelResponse.maskSecret(""));
    }

    @Test
    void createsChannelAndAutoGeneratesSecretWhenOmitted() {
        CreateNotificationChannelRequest request = new CreateNotificationChannelRequest(
                "Ops Slack Channel",
                NotificationChannelType.WEBHOOK,
                "https://hooks.slack.com/services/test",
                null,
                DisruptionSeverity.HIGH,
                true
        );

        when(channels.save(any(NotificationChannel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationChannelResponse response = service.createChannel(COMPANY_ID, request);

        assertEquals("Ops Slack Channel", response.name());
        assertEquals(NotificationChannelType.WEBHOOK, response.channelType());
        assertEquals("https://hooks.slack.com/services/test", response.targetEndpoint());
        assertEquals(DisruptionSeverity.HIGH, response.minimumSeverity());
        assertTrue(response.active());
        assertNotNull(response.secretKey());
    }

    @Test
    void testChannelDispatchesSignedTestNotificationAndPersistsLog() {
        NotificationChannel channel = NotificationChannel.create(
                COMPANY_ID,
                "Emergency PagerDuty",
                NotificationChannelType.WEBHOOK,
                "https://events.pagerduty.com/v2/enqueue",
                "pd_secret_key_12345",
                DisruptionSeverity.HIGH,
                true
        );

        when(channels.findByIdAndCompanyId(CHANNEL_ID, COMPANY_ID)).thenReturn(Optional.of(channel));
        when(webhookDispatcher.dispatch(eq(channel.getTargetEndpoint()), any(), any()))
                .thenReturn(WebhookDispatchResult.ok(200, "Delivered successfully (HTTP 200)"));

        NotificationTestResultResponse result = service.testChannel(COMPANY_ID, CHANNEL_ID);

        assertEquals(CHANNEL_ID, result.channelId());
        assertEquals(NotificationDeliveryStatus.DELIVERED, result.status());
        assertNotNull(result.signature());
        assertTrue(result.payload().contains("TEST_NOTIFICATION"));

        verify(webhookDispatcher).dispatch(eq(channel.getTargetEndpoint()), eq(result.signature()), eq(result.payload()));
        verify(deliveryLogs).save(any(NotificationDeliveryLog.class));
    }

    @Test
    void dispatchesAlertsOnlyToChannelsMatchingSeverityThreshold() {
        NotificationChannel highChannel = NotificationChannel.create(
                COMPANY_ID,
                "Critical Escalation",
                NotificationChannelType.WEBHOOK,
                "https://alerts.example.com/high",
                "secret_high_9999",
                DisruptionSeverity.HIGH,
                true
        );

        when(channels.findByCompanyIdAndActiveTrue(COMPANY_ID)).thenReturn(List.of(highChannel));
        when(webhookDispatcher.dispatch(any(), any(), any()))
                .thenReturn(WebhookDispatchResult.ok(200, "OK"));

        // Event 1: MEDIUM severity -> should be SUPPRESSED for HIGH channel
        DisruptionEvent mediumEvent = new DisruptionEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                DisruptionType.ROUTE_DEVIATION,
                DisruptionSeverity.MEDIUM,
                NOW,
                true,
                List.of("MINOR_DETOUR"),
                Map.of()
        );

        service.dispatchDisruptionAlerts(COMPANY_ID, mediumEvent);

        verify(webhookDispatcher, never()).dispatch(any(), any(), any());
        verify(deliveryLogs).save(any(NotificationDeliveryLog.class));

        // Event 2: CRITICAL severity -> matches HIGH channel threshold -> dispatched!
        DisruptionEvent criticalEvent = new DisruptionEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                DisruptionType.VEHICLE_OVERHEATING,
                DisruptionSeverity.CRITICAL,
                NOW,
                true,
                List.of("COOLANT_LEAK"),
                Map.of("tempCelsius", 112)
        );

        service.dispatchDisruptionAlerts(COMPANY_ID, criticalEvent);

        verify(webhookDispatcher).dispatch(eq(highChannel.getTargetEndpoint()), any(), any());
    }
}
