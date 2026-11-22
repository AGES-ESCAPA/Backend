package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.escapa.backend.application.port.EmailChangeTokenRepositoryPort;
import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.infrastructure.persistence.entity.EmailChangeTokenEntity;

@Repository
public class EmailChangeTokenRepositoryAdapter implements EmailChangeTokenRepositoryPort {

    private final EmailChangeTokenJpaRepository emailChangeTokenJpaRepository;

    public EmailChangeTokenRepositoryAdapter(EmailChangeTokenJpaRepository emailChangeTokenJpaRepository) {
        this.emailChangeTokenJpaRepository = emailChangeTokenJpaRepository;
    }

    @Override
    public EmailChangeToken save(EmailChangeToken token) {
        final EmailChangeTokenEntity entity;
        if (token.getId() != null) {
            entity = emailChangeTokenJpaRepository.findById(token.getId())
                    .orElseGet(() -> EmailChangeTokenMapper.toEntity(token));
            EmailChangeTokenMapper.updateEntity(entity, token);
        } else {
            entity = EmailChangeTokenMapper.toEntity(token);
        }
        return EmailChangeTokenMapper.toDomain(emailChangeTokenJpaRepository.save(entity));
    }

    @Override
    public Optional<EmailChangeToken> findByTokenHash(String tokenHash) {
        return emailChangeTokenJpaRepository.findByTokenHash(tokenHash).map(EmailChangeTokenMapper::toDomain);
    }

    @Override
    public Optional<EmailChangeToken> findOpenByUserId(UUID userId, LocalDateTime now) {
        return emailChangeTokenJpaRepository
                .findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(userId, now)
                .map(EmailChangeTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean markAsUsed(UUID tokenId, LocalDateTime usedAt) {
        return emailChangeTokenJpaRepository.markAsUsed(tokenId, usedAt) == 1;
    }

    @Override
    @Transactional
    public void invalidateOpenTokens(UUID userId, LocalDateTime now) {
        emailChangeTokenJpaRepository.invalidateOpenByUserId(userId, now);
    }
}
