package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.UserNotification;
import com.escapa.backend.application.port.NotificationRepositoryPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Fake em memória de {@link NotificationRepositoryPort} para testes unitários.
 */
public final class InMemoryNotificationRepositoryPort implements NotificationRepositoryPort {

    private record StoredNotification(UUID userId, UserNotification notification) {
    }

    private final List<StoredNotification> notifications = new ArrayList<>();

    public UserNotification givenNotification(UUID userId, UUID courseId, UUID lessonId, LocalDateTime createdAt) {
        final UserNotification notification = new UserNotification(UUID.randomUUID(), "LESSON_UPDATED",
                "Aula atualizada", "A aula foi atualizada.", courseId, lessonId, false, createdAt);
        notifications.add(new StoredNotification(userId, notification));
        return notification;
    }

    @Override
    public List<UserNotification> findRecentByUserId(UUID userId, int limit) {
        return notifications.stream()
                .filter(stored -> stored.userId().equals(userId))
                .map(StoredNotification::notification)
                .sorted(Comparator.comparing(UserNotification::createdAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return notifications.stream()
                .filter(stored -> stored.userId().equals(userId) && !stored.notification().read())
                .count();
    }

    @Override
    public boolean markAsRead(UUID userId, UUID notificationId, LocalDateTime readAt) {
        for (int index = 0; index < notifications.size(); index++) {
            final StoredNotification stored = notifications.get(index);
            if (stored.userId().equals(userId) && stored.notification().id().equals(notificationId)) {
                notifications.set(index, new StoredNotification(userId, asRead(stored.notification())));
                return true;
            }
        }
        return false;
    }

    @Override
    public void markAllAsRead(UUID userId, LocalDateTime readAt) {
        notifications.replaceAll(stored -> stored.userId().equals(userId)
                ? new StoredNotification(userId, asRead(stored.notification()))
                : stored);
    }

    private static UserNotification asRead(UserNotification notification) {
        return new UserNotification(notification.id(), notification.type(), notification.title(),
                notification.message(), notification.courseId(), notification.lessonId(), true,
                notification.createdAt());
    }
}
