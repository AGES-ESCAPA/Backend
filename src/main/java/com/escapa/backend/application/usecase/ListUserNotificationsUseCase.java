package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.UserNotification;
import com.escapa.backend.application.port.NotificationRepositoryPort;

import java.util.List;
import java.util.UUID;

/** Lista as notificações mais recentes do usuário autenticado e quantas ainda não foram lidas. */
public class ListUserNotificationsUseCase {

    static final int MAX_NOTIFICATIONS = 50;

    private final NotificationRepositoryPort notificationRepositoryPort;

    public ListUserNotificationsUseCase(NotificationRepositoryPort notificationRepositoryPort) {
        this.notificationRepositoryPort = notificationRepositoryPort;
    }

    public Result execute(UUID userId) {
        return new Result(
                notificationRepositoryPort.findRecentByUserId(userId, MAX_NOTIFICATIONS),
                notificationRepositoryPort.countUnreadByUserId(userId));
    }

    public record Result(List<UserNotification> notifications, long unreadCount) {
    }
}
