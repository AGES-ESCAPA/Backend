package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.escapa.backend.infrastructure.persistence.entity.NotificationEntity;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query("SELECT n FROM NotificationEntity n LEFT JOIN FETCH n.course LEFT JOIN FETCH n.content "
            + "WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<NotificationEntity> findRecentByUserId(@Param("userId") UUID userId, Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.read = true, n.readAt = :readAt "
            + "WHERE n.id = :id AND n.user.id = :userId AND n.read = false")
    int markAsRead(@Param("userId") UUID userId, @Param("id") UUID id, @Param("readAt") LocalDateTime readAt);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.read = true, n.readAt = :readAt "
            + "WHERE n.user.id = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") UUID userId, @Param("readAt") LocalDateTime readAt);
}
