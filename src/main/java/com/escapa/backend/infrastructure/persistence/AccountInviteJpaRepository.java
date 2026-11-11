package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.escapa.backend.infrastructure.persistence.entity.AccountInviteEntity;

@Repository
public interface AccountInviteJpaRepository extends JpaRepository<AccountInviteEntity, UUID> {

    Optional<AccountInviteEntity> findByTokenHash(String tokenHash);

    boolean existsByUserId(UUID userId);

    @Modifying
    @Query("UPDATE AccountInviteEntity i SET i.usedAt = :usedAt WHERE i.id = :id AND i.usedAt IS NULL")
    int markAsUsed(@Param("id") UUID id, @Param("usedAt") LocalDateTime usedAt);
}
