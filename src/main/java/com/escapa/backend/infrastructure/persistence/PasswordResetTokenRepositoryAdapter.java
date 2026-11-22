package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.escapa.backend.application.port.PasswordResetTokenRepositoryPort;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.infrastructure.persistence.entity.PasswordResetTokenEntity;

@Repository
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepositoryPort {

    private final PasswordResetTokenJpaRepository passwordResetTokenJpaRepository;

    public PasswordResetTokenRepositoryAdapter(PasswordResetTokenJpaRepository passwordResetTokenJpaRepository) {
        this.passwordResetTokenJpaRepository = passwordResetTokenJpaRepository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        final PasswordResetTokenEntity entity;
        if (token.getId() != null) {
            entity = passwordResetTokenJpaRepository.findById(token.getId())
                    .orElseGet(() -> PasswordResetTokenMapper.toEntity(token));
            PasswordResetTokenMapper.updateEntity(entity, token);
        } else {
            entity = PasswordResetTokenMapper.toEntity(token);
        }
        return PasswordResetTokenMapper.toDomain(passwordResetTokenJpaRepository.save(entity));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return passwordResetTokenJpaRepository.findByTokenHash(tokenHash).map(PasswordResetTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean markAsUsed(UUID tokenId, LocalDateTime usedAt) {
        return passwordResetTokenJpaRepository.markAsUsed(tokenId, usedAt) == 1;
    }

    @Override
    @Transactional
    public void invalidateOpenTokens(UUID userId, LocalDateTime now) {
        passwordResetTokenJpaRepository.invalidateOpenByUserId(userId, now);
    }
}
