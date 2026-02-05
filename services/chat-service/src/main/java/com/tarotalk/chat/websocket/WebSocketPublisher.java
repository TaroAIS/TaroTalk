package com.tarotalk.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarotalk.chat.domain.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

@Component
public class WebSocketPublisher {
    private static final Logger log = LoggerFactory.getLogger(WebSocketPublisher.class);

    private final WebSocketRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebSocketPublisher(WebSocketRegistry registry) {
        this.registry = registry;
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
}
