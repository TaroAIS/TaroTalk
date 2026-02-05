package com.tarotalk.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class LlmClient {
    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public LlmClient(RestTemplate restTemplate,
                     @Value("${ai.provider.base-url:}") String baseUrl,
                     @Value("${ai.provider.api-key:}") String apiKey,
                     @Value("${ai.provider.model:gpt-4o-mini}") String model) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String generateText(String systemPrompt, String userPrompt) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return "[stub] " + userPrompt;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.setBearerAuth(apiKey);
        }
        Map<String, Object> systemMessage = new java.util.HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        Map<String, Object> userMessage = new java.util.HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("model", model);
        payload.put("messages", java.util.Arrays.asList(systemMessage, userMessage));
        try {
            Map<?, ?> response = restTemplate.postForObject(baseUrl, new HttpEntity<>(payload, headers), Map.class);
            return extractText(response);
        } catch (Exception ex) {
            log.warn("LLM request failed: {}", ex.getMessage());
            return "[fallback] " + userPrompt;
        }
    }

    private String extractText(Map<?, ?> response) {
        if (response == null) {
            return "";
        }
        Object choicesObj = response.get("choices");
        if (choicesObj instanceof List) {
            List<?> choices = (List<?>) choicesObj;
            if (!choices.isEmpty() && choices.get(0) instanceof Map) {
                Map<?, ?> first = (Map<?, ?>) choices.get(0);
                Object text = first.get("text");
                if (text != null) {
                    return text.toString().trim();
                }
                Object message = first.get("message");
                if (message instanceof Map) {
                    Object content = ((Map<?, ?>) message).get("content");
                    if (content != null) {
                        return content.toString().trim();
                    }
                }
            }
        }
        Object content = response.get("content");
        return content == null ? response.toString() : content.toString();
    }
}
