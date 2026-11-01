package com.escapa.backend.application.port;

import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.domain.user.AccountInvite;

public interface AccountInviteRepositoryPort {

    /** Insere o convite (sem id) ou atualiza um existente, como ao marcá-lo como usado. */
    AccountInvite save(AccountInvite invite);

    Optional<AccountInvite> findByTokenHash(String tokenHash);

    /** Vale para convites usados ou não: identifica quem já foi colaborador. */
    boolean existsByUserId(UUID userId);
}
