package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.UserNotification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String message,
        UUID courseId,
        UUID lessonId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(UserNotification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.type(),
                notification.title(),
                notification.message(),
                notification.courseId(),
                notification.lessonId(),
                notification.read(),
                notification.createdAt());
    }
}
