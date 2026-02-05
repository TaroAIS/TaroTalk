package com.tarotalk.chat.config;

import com.tarotalk.chat.websocket.ChatWebSocketHandler;
import com.tarotalk.chat.websocket.WebSocketRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketRegistry registry;

    public WebSocketConfig(WebSocketRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(this.registry), "/ws/chat").setAllowedOrigins("*");
    }
}
