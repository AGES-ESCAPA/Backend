package com.escapa.backend.application.usecase;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.domain.user.AccountInvite;

/** Guarda cópias, como o banco: alterar o convite sem chamar save não muda o que está gravado. */
public final class InMemoryAccountInviteRepositoryPort implements AccountInviteRepositoryPort {

    private final Map<UUID, AccountInvite> invites = new LinkedHashMap<>();

    @Override
    public AccountInvite save(AccountInvite invite) {
        // Espelha o adapter real: id nulo vira um UUID gerado na persistencia.
        final UUID id = invite.getId() != null ? invite.getId() : UUID.randomUUID();
        invites.put(id, copy(invite, id));
        return copy(invite, id);
    }

    @Override
    public Optional<AccountInvite> findByTokenHash(String tokenHash) {
        return invites.values().stream()
                .filter(invite -> invite.getTokenHash().equals(tokenHash))
                .findFirst()
                .map(invite -> copy(invite, invite.getId()));
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return invites.values().stream().anyMatch(invite -> invite.getUserId().equals(userId));
    }

    private static AccountInvite copy(AccountInvite invite, UUID id) {
        return new AccountInvite(id, invite.getUserId(), invite.getCompanyId(), invite.getTokenHash(),
                invite.getExpiresAt(), invite.getUsedAt(), invite.getCreatedAt());
    }
}
