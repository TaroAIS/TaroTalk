package com.tarotalk.chat.service;

import com.tarotalk.chat.api.MessageContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OrchestratorClient {
    private final RestTemplate restTemplate;
    private final String orchestratorUrl;

    public OrchestratorClient(RestTemplate restTemplate,
                              @Value("${integrations.orchestrator.base-url:}") String orchestratorUrl) {
        this.restTemplate = restTemplate;
        this.orchestratorUrl = orchestratorUrl;
    }

    public String generateReply(String conversationId, String personaSummary, List<MessageContext> context) {
        if (orchestratorUrl == null || orchestratorUrl.trim().isEmpty()) {
            return "";
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversation_id", conversationId);
        payload.put("participants", java.util.Collections.emptyList());
        payload.put("persona_summary", personaSummary);
        payload.put("messages", context);
        Map response = restTemplate.postForObject(orchestratorUrl + "/a2a/chat", payload, Map.class);
        if (response == null) {
            return "";
        }
        Object reply = response.get("reply");
        if (reply == null && response.get("data") instanceof Map) {
            reply = ((Map) response.get("data")).get("reply");
        }
        return reply == null ? "" : reply.toString();
    }
}
