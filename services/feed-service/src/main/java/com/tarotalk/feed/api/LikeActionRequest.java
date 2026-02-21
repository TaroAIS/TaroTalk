package com.tarotalk.feed.api;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class LikeActionRequest {
    public enum Action {
        LIKE,
        UNLIKE
    }

    @NotNull
    private UUID userId;

    @NotNull
    private Action action;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }
}
