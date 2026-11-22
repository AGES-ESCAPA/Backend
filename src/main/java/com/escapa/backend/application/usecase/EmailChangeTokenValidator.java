package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import com.escapa.backend.application.port.EmailChangeTokenRepositoryPort;
import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.domain.user.EmailChangeTokenExpiredException;
import com.escapa.backend.domain.user.EmailChangeTokenNotFoundException;

public class EmailChangeTokenValidator {

    private final EmailChangeTokenRepositoryPort emailChangeTokenRepositoryPort;
    private final Clock clock;

    public EmailChangeTokenValidator(
            EmailChangeTokenRepositoryPort emailChangeTokenRepositoryPort,
            Clock clock
    ) {
        this.emailChangeTokenRepositoryPort = emailChangeTokenRepositoryPort;
        this.clock = clock;
    }

    public EmailChangeToken requireOpenToken(String token) {
        if (token == null || token.isBlank()) {
            throw new EmailChangeTokenNotFoundException();
        }
        final EmailChangeToken changeToken = emailChangeTokenRepositoryPort
                .findByTokenHash(EmailChangeToken.hashToken(token))
                .orElseThrow(EmailChangeTokenNotFoundException::new);
        if (changeToken.isUsed()) {
            throw new EmailChangeTokenNotFoundException();
        }
        if (changeToken.isExpired(LocalDateTime.now(clock))) {
            throw new EmailChangeTokenExpiredException();
        }
        return changeToken;
    }
}
