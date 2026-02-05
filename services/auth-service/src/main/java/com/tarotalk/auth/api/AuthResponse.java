package com.tarotalk.auth.api;

import java.time.Instant;
import java.util.UUID;

public class AuthResponse {
    private UUID userId;
    private String accessToken;
    private Instant expiresAt;

    public AuthResponse() {
    }

    public AuthResponse(UUID userId, String accessToken, Instant expiresAt) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
