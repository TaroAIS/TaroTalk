package com.tarotalk.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tarotalk.chat.domain.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Set;

@Component
public class WebSocketPublisher {
    private static final Logger log = LoggerFactory.getLogger(WebSocketPublisher.class);

    private final WebSocketRegistry registry;
    private final ObjectMapper objectMapper;

    public WebSocketPublisher(WebSocketRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void publish(String conversationId, ChatMessage message) {
        Set<WebSocketSession> sessions = registry.getSessions(conversationId);
        if (sessions.isEmpty()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (Exception ex) {
            log.warn("websocket publish failed: {}", ex.getMessage());
        }
    }

    public void publishEvent(String conversationId, String eventType, Object data) {
        Set<WebSocketSession> sessions = registry.getSessions(conversationId);
        if (sessions.isEmpty()) {
            return;
        }
        try {
            HashMap<String, Object> payload = new HashMap<>();
            payload.put("type", eventType);
            payload.put("data", data);
            String json = objectMapper.writeValueAsString(payload);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception ex) {
            log.warn("websocket publish event failed: {}", ex.getMessage());
        }
    }
}
