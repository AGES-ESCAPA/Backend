package com.escapa.backend.application.port;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.domain.user.AccountInvite;

public interface AccountInviteRepositoryPort {

    /** Insere o convite (sem id) ou atualiza um existente, como ao marcá-lo como usado. */
    AccountInvite save(AccountInvite invite);

    Optional<AccountInvite> findByTokenHash(String tokenHash);

    /**
     * Marca o convite como usado só se ainda estiver em aberto, numa única instrução atômica.
     * Devolve {@code false} se outra requisição já o usou: duas requisições com o mesmo token
     * não conseguem criar a senha ao mesmo tempo.
     */
    boolean markAsUsed(UUID inviteId, LocalDateTime usedAt);

    /** Encerra os convites ainda em aberto do usuário (como se usados): o link já enviado deixa de valer. */
    void invalidateOpenInvites(UUID userId, LocalDateTime now);

    /** Vale para convites usados ou não: identifica quem já foi colaborador. */
    boolean existsByUserId(UUID userId);
}
