package com.tarotalk.persona.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
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

    public void bootstrapAgents(String userId, String personaSummary, String traitsJson) {
        if (orchestratorUrl == null || orchestratorUrl.trim().isEmpty()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("persona_summary", personaSummary);
        payload.put("traits", parseTraits(traitsJson));
        payload.put("agent_count", 5);
        restTemplate.postForObject(orchestratorUrl + "/a2a/bootstrap", payload, Map.class);
    }

    private Map<String, Object> parseTraits(String traitsJson) {
        if (traitsJson == null || traitsJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(traitsJson, Map.class);
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }
}
