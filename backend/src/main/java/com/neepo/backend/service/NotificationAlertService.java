package com.neepo.backend.service;

import com.neepo.backend.domain.NotificationDeliveryStatus;
import com.neepo.backend.dto.CreateNotificationChannelRequest;
import com.neepo.backend.dto.NotificationChannelResponse;
import com.neepo.backend.dto.NotificationDeliveryLogResponse;
import com.neepo.backend.dto.NotificationTestResultResponse;
import com.neepo.backend.entity.NotificationChannel;
import com.neepo.backend.entity.NotificationDeliveryLog;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.NotificationChannelRepository;
import com.neepo.backend.repository.NotificationDeliveryLogRepository;
import com.neepo.contracts.DisruptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationAlertService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationAlertService.class);

    private final NotificationChannelRepository channels;
    private final NotificationDeliveryLogRepository deliveryLogs;
    private final WebhookDispatcher webhookDispatcher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NotificationAlertService(
            NotificationChannelRepository channels,
            NotificationDeliveryLogRepository deliveryLogs,
            WebhookDispatcher webhookDispatcher,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.channels = channels;
        this.deliveryLogs = deliveryLogs;
        this.webhookDispatcher = webhookDispatcher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public List<NotificationChannelResponse> listChannels(UUID companyId) {
        return channels.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(NotificationChannelResponse::from)
                .toList();
    }

    @Transactional
    public NotificationChannelResponse createChannel(UUID companyId, CreateNotificationChannelRequest request) {
        String secret = (request.secretKey() != null && !request.secretKey().isBlank())
                ? request.secretKey()
                : "whsec_" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 8);

        NotificationChannel channel = NotificationChannel.create(
                companyId,
                request.name(),
                request.channelType(),
                request.targetEndpoint(),
                secret,
                request.minimumSeverity(),
                request.isActiveOrTrue()
        );

        NotificationChannel saved = channels.save(channel);
        LOGGER.info("Created notification channel id={} companyId={} type={}", saved.getId(), companyId, saved.getChannelType());
        return NotificationChannelResponse.from(saved);
    }

    @Transactional
    public void deleteChannel(UUID companyId, UUID channelId) {
        NotificationChannel channel = channels.findByIdAndCompanyId(channelId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("NOTIFICATION_CHANNEL_NOT_FOUND", "Notification channel not found"));
        channels.delete(channel);
        LOGGER.info("Deleted notification channel id={} companyId={}", channelId, companyId);
    }

    @Transactional
    public NotificationTestResultResponse testChannel(UUID companyId, UUID channelId) {
        NotificationChannel channel = channels.findByIdAndCompanyId(channelId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("NOTIFICATION_CHANNEL_NOT_FOUND", "Notification channel not found"));

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("event", "TEST_NOTIFICATION");
        payloadMap.put("channelId", channelId.toString());
        payloadMap.put("companyId", companyId.toString());
        payloadMap.put("channelName", channel.getName());
        payloadMap.put("minimumSeverity", channel.getMinimumSeverity().name());
        payloadMap.put("timestamp", clock.instant().toString());
        payloadMap.put("message", "NEEPO-CARE real-time alert engine test ping");

        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            payload = "{\"event\":\"TEST_NOTIFICATION\",\"channelId\":\"" + channelId + "\"}";
        }

        String signature = calculateHmacSha256(channel.getSecretKey(), payload);
        WebhookDispatchResult dispatchResult = webhookDispatcher.dispatch(channel.getTargetEndpoint(), signature, payload);

        NotificationDeliveryStatus status = dispatchResult.success()
                ? NotificationDeliveryStatus.DELIVERED
                : NotificationDeliveryStatus.FAILED;

        NotificationDeliveryLog log = NotificationDeliveryLog.create(
                channelId,
                companyId,
                null,
                "TEST_PING",
                channel.getMinimumSeverity(),
                status,
                signature,
                payload,
                dispatchResult.success() ? null : dispatchResult.message()
        );
        deliveryLogs.save(log);

        return new NotificationTestResultResponse(
                channelId,
                status,
                dispatchResult.message(),
                signature,
                payload
        );
    }

    public List<NotificationDeliveryLogResponse> getDeliveryLogs(UUID companyId, int limit) {
        int boundedLimit = Math.min(Math.max(1, limit), 200);
        return deliveryLogs.findByCompanyIdOrderByDeliveredAtDesc(companyId, PageRequest.of(0, boundedLimit))
                .stream()
                .map(NotificationDeliveryLogResponse::from)
                .toList();
    }

    @Transactional
    public void dispatchDisruptionAlerts(UUID companyId, DisruptionEvent disruption) {
        List<NotificationChannel> activeChannels = channels.findByCompanyIdAndActiveTrue(companyId);
        if (activeChannels.isEmpty()) {
            LOGGER.debug("No active notification channels configured for companyId={}", companyId);
            return;
        }

        Map<String, Object> alertMap = new LinkedHashMap<>();
        alertMap.put("event", "DISRUPTION_DETECTED");
        alertMap.put("disruptionId", disruption.id().toString());
        alertMap.put("disruptionEventId", disruption.eventId().toString());
        alertMap.put("tripId", disruption.tripId().toString());
        alertMap.put("type", disruption.type().name());
        alertMap.put("severity", disruption.severity().name());
        alertMap.put("detectedAt", disruption.detectedAt().toString());
        alertMap.put("explanationCodes", disruption.explanationCodes());
        alertMap.put("evidence", disruption.evidence());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(alertMap);
        } catch (Exception e) {
            payload = "{\"event\":\"DISRUPTION_DETECTED\",\"type\":\"" + disruption.type() + "\"}";
        }

        for (NotificationChannel channel : activeChannels) {
            boolean severityMatches = disruption.severity().ordinal() >= channel.getMinimumSeverity().ordinal();
            if (!severityMatches) {
                NotificationDeliveryLog suppressedLog = NotificationDeliveryLog.create(
                        channel.getId(),
                        companyId,
                        disruption.eventId(),
                        disruption.type().name(),
                        disruption.severity(),
                        NotificationDeliveryStatus.SUPPRESSED_BY_SEVERITY,
                        null,
                        payload,
                        "Suppressed: disruption severity " + disruption.severity()
                                + " is below channel minimum threshold " + channel.getMinimumSeverity()
                );
                deliveryLogs.save(suppressedLog);
                continue;
            }

            String signature = calculateHmacSha256(channel.getSecretKey(), payload);
            WebhookDispatchResult result = webhookDispatcher.dispatch(channel.getTargetEndpoint(), signature, payload);

            NotificationDeliveryStatus status = result.success()
                    ? NotificationDeliveryStatus.DELIVERED
                    : NotificationDeliveryStatus.FAILED;

            NotificationDeliveryLog deliveryLog = NotificationDeliveryLog.create(
                    channel.getId(),
                    companyId,
                    disruption.eventId(),
                    disruption.type().name(),
                    disruption.severity(),
                    status,
                    signature,
                    payload,
                    result.success() ? null : result.message()
            );
            deliveryLogs.save(deliveryLog);
            LOGGER.info("Dispatched alert channelId={} status={} disruptionType={}", channel.getId(), status, disruption.type());
        }
    }

    public static String calculateHmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", e);
        }
    }
}
