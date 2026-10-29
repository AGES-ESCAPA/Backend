package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.NotificationRepositoryPort;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public class MarkAllNotificationsAsReadUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final Clock clock;

    public MarkAllNotificationsAsReadUseCase(NotificationRepositoryPort notificationRepositoryPort, Clock clock) {
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.clock = clock;
    }

    public void execute(UUID userId) {
        notificationRepositoryPort.markAllAsRead(userId, LocalDateTime.now(clock));
    }
}
