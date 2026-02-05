package com.tarotalk.chat.service;

import com.tarotalk.chat.api.MessageContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiClient {
    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public AiClient(RestTemplate restTemplate,
                    @Value("${integrations.ai-service.base-url}") String aiServiceUrl) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
    }

    public String generateReply(String personaSummary, String conversationId, List<MessageContext> context) {
        if (aiServiceUrl == null || aiServiceUrl.trim().isEmpty()) {
            return "[stub]";
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("personaSummary", personaSummary);
        payload.put("conversationId", conversationId);
        payload.put("messages", context);
        Map response = restTemplate.postForObject(aiServiceUrl + "/api/ai/reply", payload, Map.class);
        if (response == null) {
            return "";
        }
        Object data = response.get("data");
        if (data instanceof Map) {
            Object reply = ((Map) data).get("replyText");
            return reply == null ? "" : reply.toString();
        }
        return "";
    }
}
