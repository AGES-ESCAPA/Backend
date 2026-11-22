package com.escapa.backend.application.port;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.domain.user.PasswordResetToken;

public interface PasswordResetTokenRepositoryPort {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Marca o token como usado so se ainda estiver em aberto. Devolve {@code false} se
     * outra requisicao ja o usou.
     */
    boolean markAsUsed(UUID tokenId, LocalDateTime usedAt);

    /** Expira tokens ainda em aberto do usuario: o link ja enviado passa a responder como expirado. */
    void invalidateOpenTokens(UUID userId, LocalDateTime now);
}
