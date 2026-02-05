package com.tarotalk.chat.websocket;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketRegistry registry;

    public ChatWebSocketHandler(WebSocketRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String conversationId = extractConversationId(session.getUri());
        if (conversationId != null) {
            registry.addSession(conversationId, session);
            session.sendMessage(new TextMessage("connected"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String conversationId = extractConversationId(session.getUri());
        if (conversationId != null) {
            registry.removeSession(conversationId, session);
        }
    }

    private String extractConversationId(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        String[] params = uri.getQuery().split("&");
        for (String param : params) {
            String[] pair = param.split("=");
            if (pair.length == 2 && "conversationId".equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }
}
