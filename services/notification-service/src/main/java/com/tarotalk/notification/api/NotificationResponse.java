package com.tarotalk.notification.api;

import com.tarotalk.notification.domain.Notification;

import java.time.Instant;
import java.util.UUID;

public class NotificationResponse {
    private UUID notificationId;
    private UUID userId;
    private Notification.Type type;
    private String title;
    private String content;
    private Notification.Status status;
    private Instant createdAt;

    public static NotificationResponse from(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.notificationId = notification.getNotificationId();
        response.userId = notification.getUserId();
        response.type = notification.getType();
        response.title = notification.getTitle();
        response.content = notification.getContent();
        response.status = notification.getStatus();
        response.createdAt = notification.getCreatedAt();
        return response;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Notification.Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Notification.Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
