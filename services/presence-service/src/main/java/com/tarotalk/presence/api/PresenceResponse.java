package com.tarotalk.presence.api;

import java.time.Instant;
import java.util.UUID;

public class PresenceResponse {
    private UUID userId;
    private String status;
    private Instant lastSeen;

    public PresenceResponse() {
    }

    public PresenceResponse(UUID userId, String status, Instant lastSeen) {
        this.userId = userId;
        this.status = status;
        this.lastSeen = lastSeen;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }
}
