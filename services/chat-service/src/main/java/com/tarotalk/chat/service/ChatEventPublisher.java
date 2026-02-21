package com.tarotalk.chat.service;

import com.tarotalk.chat.domain.ChatMessage;
import com.tarotalk.common.web.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ChatEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);

    private final RestTemplate restTemplate;
    private final String eventServiceUrl;

    public ChatEventPublisher(RestTemplate restTemplate,
                              @Value("${integrations.event-service.base-url:}") String eventServiceUrl) {
        this.restTemplate = restTemplate;
        this.eventServiceUrl = eventServiceUrl;
    }

    public String publishMessageSaved(ChatMessage message, String traceId) {
        if (eventServiceUrl == null || eventServiceUrl.trim().isEmpty() || message == null) {
            return null;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("messageId", message.getMessageId());
        payload.put("conversationId", message.getConversationId() == null ? null : message.getConversationId().toString());
        payload.put("senderId", message.getSenderId() == null ? null : message.getSenderId().toString());
        payload.put("content", message.getContent());
        payload.put("role", message.getRole());
        payload.put("round", message.getRound());
        payload.put("source", message.getSource());
        payload.put("effectRef", message.getEffectRef());

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "CHAT_MESSAGE_CREATED");
        request.put("actorId", message.getSenderId() == null ? null : message.getSenderId().toString());
        request.put("targetId", message.getConversationId() == null ? null : message.getConversationId().toString());
        request.put("entityType", "CONVERSATION");
        request.put("entityId", message.getConversationId() == null ? null : message.getConversationId().toString());
        request.put("sourceService", "chat-service");
        request.put("traceId", traceId == null || traceId.trim().isEmpty() ? TraceContext.currentTraceIdOrRandom() : traceId);
        request.put("idempotencyKey", "CHAT_MESSAGE_CREATED:" + safe(message.getMessageId()));
        request.put("payload", payload);

        try {
            Map response = restTemplate.postForObject(eventServiceUrl + "/internal/events", request, Map.class);
            if (response == null) {
                return null;
            }
            Object data = response.get("data");
            if (data instanceof Map) {
                Object eventId = ((Map<?, ?>) data).get("eventId");
                return eventId == null ? null : String.valueOf(eventId);
            }
            Object eventId = response.get("eventId");
            return eventId == null ? null : String.valueOf(eventId);
        } catch (Exception ex) {
            log.warn("publish chat event failed: {}", ex.getMessage());
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "none" : value;
    }
}
