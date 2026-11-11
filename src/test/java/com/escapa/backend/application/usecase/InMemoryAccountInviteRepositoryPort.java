package com.escapa.backend.application.usecase;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.domain.user.AccountInvite;

/** Guarda cópias, como o banco: alterar o convite sem chamar save não muda o que está gravado. */
public final class InMemoryAccountInviteRepositoryPort implements AccountInviteRepositoryPort {

    private final Map<UUID, AccountInvite> invites = new LinkedHashMap<>();
    private LocalDateTime concurrentUseAt;

    @Override
    public AccountInvite save(AccountInvite invite) {
        // Espelha o adapter real: id nulo vira um UUID gerado na persistencia.
        final UUID id = invite.getId() != null ? invite.getId() : UUID.randomUUID();
        invites.put(id, copy(invite, id));
        return copy(invite, id);
    }

    @Override
    public Optional<AccountInvite> findByTokenHash(String tokenHash) {
        final Optional<AccountInvite> found = invites.values().stream()
                .filter(invite -> invite.getTokenHash().equals(tokenHash))
                .findFirst();
        final Optional<AccountInvite> result = found.map(invite -> copy(invite, invite.getId()));
        if (concurrentUseAt != null) {
            found.ifPresent(invite -> invite.markAsUsed(concurrentUseAt));
            concurrentUseAt = null;
        }
        return result;
    }

    @Override
    public boolean markAsUsed(UUID inviteId, LocalDateTime usedAt) {
        final AccountInvite stored = invites.get(inviteId);
        if (stored == null || stored.isUsed()) {
            return false;
        }
        stored.markAsUsed(usedAt);
        return true;
    }

    /**
     * Simula outra requisição com o mesmo token: a próxima busca devolve o convite ainda em
     * aberto, mas o gravado passa a constar como usado logo em seguida.
     */
    public void simulateConcurrentUseAfterNextLookup(LocalDateTime usedAt) {
        this.concurrentUseAt = usedAt;
    }

    @Override
    public void invalidateOpenInvites(UUID userId, LocalDateTime now) {
        invites.values().stream()
                .filter(invite -> invite.getUserId().equals(userId) && !invite.isUsed())
                .forEach(invite -> invite.markAsUsed(now));
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
