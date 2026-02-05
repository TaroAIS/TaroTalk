package com.tarotalk.persona.service;

import com.tarotalk.persona.service.dto.PersonaGenerationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
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

    public PersonaGenerationResult generatePersona(String userId, String description) {
        if (aiServiceUrl == null || aiServiceUrl.trim().isEmpty()) {
            return PersonaGenerationResult.stub(description);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("description", description);
        Map response = restTemplate.postForObject(aiServiceUrl + "/api/ai/persona", payload, Map.class);
        return PersonaGenerationResult.fromResponse(response, description);
    }
}
