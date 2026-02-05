package com.tarotalk.presence.api;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class HeartbeatRequest {
    @NotNull
    private UUID userId;
    private String status = "ONLINE";

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
