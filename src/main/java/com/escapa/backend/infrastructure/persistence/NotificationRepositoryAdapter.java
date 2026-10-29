package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.UserNotification;
import com.escapa.backend.application.port.NotificationRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository notificationJpaRepository;

    public NotificationRepositoryAdapter(NotificationJpaRepository notificationJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserNotification> findRecentByUserId(UUID userId, int limit) {
        return notificationJpaRepository.findRecentByUserId(userId, PageRequest.of(0, limit)).stream()
                .map(NotificationRepositoryAdapter::toModel)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadByUserId(UUID userId) {
        return notificationJpaRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public boolean markAsRead(UUID userId, UUID notificationId, LocalDateTime readAt) {
        // Ja lida tambem conta como sucesso: a operacao e idempotente.
        return notificationJpaRepository.markAsRead(userId, notificationId, readAt) > 0
                || notificationJpaRepository.existsByIdAndUserId(notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId, LocalDateTime readAt) {
        notificationJpaRepository.markAllAsRead(userId, readAt);
    }

    private static UserNotification toModel(NotificationEntity entity) {
        return new UserNotification(
                entity.getId(),
                entity.getType().name(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getCourse() == null ? null : entity.getCourse().getId(),
                entity.getContent() == null ? null : entity.getContent().getId(),
                Boolean.TRUE.equals(entity.getRead()),
                entity.getCreatedAt());
    }
}
