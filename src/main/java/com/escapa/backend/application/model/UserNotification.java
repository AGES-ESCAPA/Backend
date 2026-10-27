package com.escapa.backend.application.model;

import java.time.LocalDateTime;
import java.util.UUID;

/** Notificação exibida ao usuário. {@code courseId} e {@code lessonId} são nulos quando não se aplicam. */
public record UserNotification(
        UUID id,
        String type,
        String title,
        String message,
        UUID courseId,
        UUID lessonId,
        boolean read,
        LocalDateTime createdAt
) {
}
