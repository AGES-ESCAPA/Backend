package com.escapa.backend.application.usecase;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.port.PasswordResetTokenRepositoryPort;
import com.escapa.backend.domain.user.PasswordResetToken;

/** Fake em memória do repositório de tokens de recuperação de senha. */
public final class InMemoryPasswordResetTokenRepositoryPort implements PasswordResetTokenRepositoryPort {

    private final Map<UUID, PasswordResetToken> byId = new HashMap<>();
    private final Map<String, UUID> idByHash = new HashMap<>();

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        final UUID id = token.getId() != null ? token.getId() : UUID.randomUUID();
        final PasswordResetToken stored = new PasswordResetToken(
                id,
                token.getUserId(),
                token.getTokenHash(),
                token.getExpiresAt(),
                token.getUsedAt(),
                token.getCreatedAt() != null ? token.getCreatedAt() : LocalDateTime.now());
        byId.put(id, stored);
        idByHash.put(stored.getTokenHash(), id);
        return stored;
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        final UUID id = idByHash.get(tokenHash);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @Override
    public boolean markAsUsed(UUID tokenId, LocalDateTime usedAt) {
        final PasswordResetToken current = byId.get(tokenId);
        if (current == null || current.isUsed()) {
            return false;
        }
        current.markAsUsed(usedAt);
        return true;
    }

    @Override
    public void invalidateOpenTokens(UUID userId, LocalDateTime now) {
        byId.values().stream()
                .filter(token -> token.getUserId().equals(userId) && !token.isUsed() && !token.isExpired(now))
                .forEach(token -> token.markAsUsed(now));
    }
}
