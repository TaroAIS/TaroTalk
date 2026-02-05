package com.tarotalk.chat.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketRegistry {
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByConversation = new ConcurrentHashMap<>();

    public void addSession(String conversationId, WebSocketSession session) {
        sessionsByConversation.computeIfAbsent(conversationId, key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void removeSession(String conversationId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByConversation.get(conversationId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    public Set<WebSocketSession> getSessions(String conversationId) {
        return sessionsByConversation.getOrDefault(conversationId, Collections.emptySet());
    }
}
