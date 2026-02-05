package com.tarotalk.chat.api;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class TypingRequest {
    @NotNull
    private UUID userId;
    private boolean typing;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }
}
