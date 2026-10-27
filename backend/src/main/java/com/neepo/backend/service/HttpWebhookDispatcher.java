package com.neepo.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HttpWebhookDispatcher implements WebhookDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpWebhookDispatcher.class);

    private final HttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public HttpWebhookDispatcher() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build());
    }

    public HttpWebhookDispatcher(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public WebhookDispatchResult dispatch(String targetEndpoint, String signature, String payload) {
        if (targetEndpoint == null || targetEndpoint.isBlank()) {
            return WebhookDispatchResult.failure(400, "Target endpoint cannot be empty");
        }

        // Handle simulated/mock protocols (SMS, WebPush or mock endpoints)
        if (!targetEndpoint.startsWith("http://") && !targetEndpoint.startsWith("https://")) {
            LOGGER.info("Non-HTTP notification dispatched to mock/carrier gateway: {}", targetEndpoint);
            return WebhookDispatchResult.ok(200, "Dispatched to simulated channel endpoint: " + targetEndpoint);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetEndpoint))
                    .timeout(Duration.ofSeconds(4))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "NEEPO-CARE-Webhook-Engine/1.0")
                    .header("X-Neepo-Signature", signature != null ? signature : "")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                return WebhookDispatchResult.ok(code, "Delivered successfully (HTTP " + code + ")");
            }
            return WebhookDispatchResult.failure(code, "Target endpoint returned error status: HTTP " + code);
        } catch (Exception exception) {
            LOGGER.warn("Failed to dispatch webhook to {}: {}", targetEndpoint, exception.getMessage());
            return WebhookDispatchResult.failure(0, "Delivery connection failure: " + exception.getMessage());
        }
    }
}
