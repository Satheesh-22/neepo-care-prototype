package com.neepo.backend.service;

import com.neepo.backend.dto.SystemEventMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class TripEventWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final Map<UUID, Map<String, WebSocketSession>> subscribers = new ConcurrentHashMap<>();

    public TripEventWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID tripId = (UUID) session.getAttributes().get("tripId");
        WebSocketSession synchronizedSession = new ConcurrentWebSocketSessionDecorator(session, 5_000, 64 * 1024);
        subscribers.computeIfAbsent(tripId, ignored -> new ConcurrentHashMap<>())
                .put(session.getId(), synchronizedSession);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID tripId = (UUID) session.getAttributes().get("tripId");
        Map<String, WebSocketSession> tripSubscribers = subscribers.get(tripId);
        if (tripSubscribers == null) {
            return;
        }
        tripSubscribers.remove(session.getId());
        if (tripSubscribers.isEmpty()) {
            subscribers.remove(tripId, tripSubscribers);
        }
    }

    public void publish(SystemEventMessage event) {
        Map<String, WebSocketSession> tripSubscribers = subscribers.get(event.tripId());
        if (tripSubscribers == null || tripSubscribers.isEmpty()) {
            return;
        }
        TextMessage message;
        try {
            message = new TextMessage(objectMapper.writeValueAsString(event));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Trip event could not be serialized", exception);
        }
        tripSubscribers.forEach((sessionId, session) -> {
            if (!session.isOpen()) {
                tripSubscribers.remove(sessionId);
                return;
            }
            try {
                session.sendMessage(message);
            } catch (IOException exception) {
                tripSubscribers.remove(sessionId);
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ignored) {
                    // The failed session has already been evicted.
                }
            }
        });
    }
}
