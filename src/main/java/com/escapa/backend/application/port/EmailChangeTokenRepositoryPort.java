package com.escapa.backend.application.port;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.domain.user.EmailChangeToken;

public interface EmailChangeTokenRepositoryPort {

    EmailChangeToken save(EmailChangeToken token);

    Optional<EmailChangeToken> findByTokenHash(String tokenHash);

    /** Token aberto (não usado e não expirado) mais recente do usuário, se houver. */
    Optional<EmailChangeToken> findOpenByUserId(UUID userId, LocalDateTime now);

    boolean markAsUsed(UUID tokenId, LocalDateTime usedAt);

    void invalidateOpenTokens(UUID userId, LocalDateTime now);
}
