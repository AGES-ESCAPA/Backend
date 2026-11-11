package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.InviteExpiredException;
import com.escapa.backend.domain.user.InviteNotFoundException;

public class AccountInviteValidator {

    private final AccountInviteRepositoryPort accountInviteRepositoryPort;
    private final Clock clock;

    public AccountInviteValidator(AccountInviteRepositoryPort accountInviteRepositoryPort, Clock clock) {
        this.accountInviteRepositoryPort = accountInviteRepositoryPort;
        this.clock = clock;
    }

    /** Convite usado dá 404 mesmo se também estiver expirado: usado não volta a valer. */
    public AccountInvite requireOpenInvite(String token) {
        if (token == null || token.isBlank()) {
            throw new InviteNotFoundException();
        }
        final AccountInvite invite = accountInviteRepositoryPort.findByTokenHash(AccountInvite.hashToken(token))
                .filter(found -> !found.isUsed())
                .orElseThrow(InviteNotFoundException::new);
        if (invite.isExpired(LocalDateTime.now(clock))) {
            throw new InviteExpiredException();
        }
        return invite;
    }
}
