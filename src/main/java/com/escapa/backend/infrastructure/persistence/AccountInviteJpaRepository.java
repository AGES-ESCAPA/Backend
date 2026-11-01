package com.escapa.backend.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.escapa.backend.infrastructure.persistence.entity.AccountInviteEntity;

@Repository
public interface AccountInviteJpaRepository extends JpaRepository<AccountInviteEntity, UUID> {

    Optional<AccountInviteEntity> findByTokenHash(String tokenHash);

    boolean existsByUserId(UUID userId);
}
