package com.tarotalk.auth.service;

import com.tarotalk.auth.api.RegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class UserProfileClient {
    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserProfileClient(RestTemplate restTemplate,
                             @Value("${integrations.user-service.base-url}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    public void createProfile(UUID userId, RegisterRequest request) {
        if (userServiceUrl == null || userServiceUrl.trim().isEmpty()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId.toString());
        payload.put("nickname", request.getNickname());
        payload.put("avatarUrl", request.getAvatarUrl());
        restTemplate.postForEntity(userServiceUrl + "/api/users", payload, Void.class);
    }
}
