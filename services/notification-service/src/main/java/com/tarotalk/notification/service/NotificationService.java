package com.tarotalk.notification.service;

import com.tarotalk.notification.api.CreateNotificationRequest;
import com.tarotalk.notification.domain.Notification;
import com.tarotalk.notification.repo.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification create(CreateNotificationRequest request) {
        Notification notification = new Notification(UUID.randomUUID(), request.getUserId(), request.getType(), request.getTitle(), request.getContent());
        return notificationRepository.save(notification);
    }

    public List<Notification> list(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
