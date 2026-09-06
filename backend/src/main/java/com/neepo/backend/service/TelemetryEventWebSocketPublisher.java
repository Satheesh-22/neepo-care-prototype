package com.neepo.backend.service;

import com.neepo.backend.dto.SystemEventMessage;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TelemetryEventWebSocketPublisher {
    private final TripEventWebSocketHandler webSocket;

    public TelemetryEventWebSocketPublisher(TripEventWebSocketHandler webSocket) {
        this.webSocket = webSocket;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterTelemetryCommit(TelemetryUpdatedEvent event) {
        var observation = event.observation();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("vehicleId", observation.vehicleId());
        payload.put("recordedAt", observation.recordedAt());
        payload.put("location", observation.location());
        payload.put("networkState", observation.networkState());
        if (observation.engineTemperatureCelsius() != null) payload.put("engineTemperatureCelsius", observation.engineTemperatureCelsius());
        if (observation.cargoTemperatureCelsius() != null) payload.put("cargoTemperatureCelsius", observation.cargoTemperatureCelsius());
        if (observation.speedKph() != null) payload.put("speedKph", observation.speedKph());
        webSocket.publish(new SystemEventMessage(
                event.eventId(), "TELEMETRY_UPDATED", observation.tripId(), observation.eventId(),
                event.occurredAt(), 1, Map.copyOf(payload)
        ));
    }
}
