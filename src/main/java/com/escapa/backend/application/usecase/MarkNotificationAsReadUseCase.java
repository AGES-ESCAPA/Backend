package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.NotificationRepositoryPort;
import com.escapa.backend.domain.notification.NotificationNotFoundException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public class MarkNotificationAsReadUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final Clock clock;

    public MarkNotificationAsReadUseCase(NotificationRepositoryPort notificationRepositoryPort, Clock clock) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.clock = clock;
    }

    /** Notificação de outro usuário responde como inexistente, sem revelar que ela existe. */
    public void execute(UUID userId, UUID notificationId) {
        if (!notificationRepositoryPort.markAsRead(userId, notificationId, LocalDateTime.now(clock))) {
            throw new NotificationNotFoundException(notificationId);
        }
    }
}
