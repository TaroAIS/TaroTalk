package com.tarotalk.feed.api;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class LikeRequest {
    @NotNull
    private UUID userId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
