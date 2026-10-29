package com.escapa.backend.application.port;

import com.escapa.backend.application.model.UserNotification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepositoryPort {

    /** Notificações mais recentes do usuário, da mais nova para a mais antiga. */
    List<UserNotification> findRecentByUserId(UUID userId, int limit);

    long countUnreadByUserId(UUID userId);

    /** Marca como lida; devolve {@code false} se a notificação não existe ou é de outro usuário. */
    boolean markAsRead(UUID userId, UUID notificationId, LocalDateTime readAt);

    void markAllAsRead(UUID userId, LocalDateTime readAt);
}
