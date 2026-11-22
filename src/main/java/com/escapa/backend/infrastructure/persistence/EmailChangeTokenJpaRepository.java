package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.escapa.backend.infrastructure.persistence.entity.EmailChangeTokenEntity;

@Repository
public interface EmailChangeTokenJpaRepository extends JpaRepository<EmailChangeTokenEntity, UUID> {

    Optional<EmailChangeTokenEntity> findByTokenHash(String tokenHash);

    Optional<EmailChangeTokenEntity> findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId, LocalDateTime now);

    @Modifying
    @Query("UPDATE EmailChangeTokenEntity t SET t.expiresAt = :now "
            + "WHERE t.userId = :userId AND t.usedAt IS NULL AND t.expiresAt > :now")
    int invalidateOpenByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE EmailChangeTokenEntity t SET t.usedAt = :usedAt WHERE t.id = :id AND t.usedAt IS NULL")
    int markAsUsed(@Param("id") UUID id, @Param("usedAt") LocalDateTime usedAt);
}
