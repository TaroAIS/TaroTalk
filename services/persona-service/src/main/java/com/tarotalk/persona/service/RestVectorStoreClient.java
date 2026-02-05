package com.tarotalk.persona.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RestVectorStoreClient implements VectorStoreClient {
    private static final Logger log = LoggerFactory.getLogger(RestVectorStoreClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RestVectorStoreClient(RestTemplate restTemplate,
                                 @Value("${vector-store.base-url:}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public void upsert(String id, double[] vector, String payload) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("id", id);
        body.put("vector", vector);
        body.put("payload", payload);
        try {
            restTemplate.postForObject(baseUrl + "/vectors", body, Map.class);
        } catch (Exception ex) {
            log.warn("vector upsert failed: {}", ex.getMessage());
        }
    }

    @Override
    public List<String> query(double[] vector, int topK) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("vector", vector);
        body.put("topK", topK);
        try {
            Map response = restTemplate.postForObject(baseUrl + "/query", body, Map.class);
            Object ids = response == null ? null : response.get("ids");
            if (ids instanceof List) {
                return (List<String>) ids;
            }
        } catch (Exception ex) {
            log.warn("vector query failed: {}", ex.getMessage());
        }
        return Collections.emptyList();
    }
}
