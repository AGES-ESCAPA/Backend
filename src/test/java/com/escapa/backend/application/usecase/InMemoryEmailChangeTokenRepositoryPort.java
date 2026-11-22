package com.escapa.backend.application.usecase;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.port.EmailChangeTokenRepositoryPort;
import com.escapa.backend.domain.user.EmailChangeToken;

public final class InMemoryEmailChangeTokenRepositoryPort implements EmailChangeTokenRepositoryPort {

    private final Map<UUID, EmailChangeToken> tokens = new HashMap<>();

    @Override
    public EmailChangeToken save(EmailChangeToken token) {
        final UUID id = token.getId() != null ? token.getId() : UUID.randomUUID();
        final EmailChangeToken stored = new EmailChangeToken(
                id,
                token.getUserId(),
                token.getNewEmail(),
                token.getTokenHash(),
                token.getRequestedBy(),
                token.getExpiresAt(),
                token.getUsedAt(),
                token.getCreatedAt() != null ? token.getCreatedAt() : LocalDateTime.now());
        tokens.put(id, stored);
        return stored;
    }

    @Override
    public Optional<EmailChangeToken> findByTokenHash(String tokenHash) {
        return tokens.values().stream()
                .filter(token -> token.getTokenHash().equals(tokenHash))
                .findFirst();
    }

    @Override
    public Optional<EmailChangeToken> findOpenByUserId(UUID userId, LocalDateTime now) {
        return tokens.values().stream()
                .filter(token -> token.getUserId().equals(userId))
                .filter(token -> !token.isUsed())
                .filter(token -> !token.isExpired(now))
                .max(Comparator.comparing(EmailChangeToken::getCreatedAt));
    }

    @Override
    public boolean markAsUsed(UUID tokenId, LocalDateTime usedAt) {
        final EmailChangeToken current = tokens.get(tokenId);
        if (current == null || current.isUsed()) {
            return false;
        }
        current.markAsUsed(usedAt);
        return true;
    }

    @Override
    public void invalidateOpenTokens(UUID userId, LocalDateTime now) {
        tokens.values().stream()
                .filter(token -> token.getUserId().equals(userId))
                .filter(token -> !token.isUsed())
                .filter(token -> !token.isExpired(now))
                .forEach(token -> token.markAsUsed(now));
    }
}
