package com.tarotalk.notification.api;

import com.tarotalk.notification.domain.Notification;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class CreateNotificationRequest {
    @NotNull
    private UUID userId;
    @NotNull
    private Notification.Type type;
    private String title;
    private String content;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Notification.Type getType() {
        return type;
    }

    public void setType(Notification.Type type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
