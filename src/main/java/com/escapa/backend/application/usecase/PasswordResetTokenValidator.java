package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import com.escapa.backend.application.port.PasswordResetTokenRepositoryPort;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.PasswordResetTokenExpiredException;
import com.escapa.backend.domain.user.PasswordResetTokenNotFoundException;

public class PasswordResetTokenValidator {

    private final PasswordResetTokenRepositoryPort passwordResetTokenRepositoryPort;
    private final Clock clock;

    public PasswordResetTokenValidator(
            PasswordResetTokenRepositoryPort passwordResetTokenRepositoryPort,
            Clock clock
    ) {
        this.passwordResetTokenRepositoryPort = passwordResetTokenRepositoryPort;
        this.clock = clock;
    }

    public PasswordResetToken requireOpenToken(String token) {
        if (token == null || token.isBlank()) {
            throw new PasswordResetTokenNotFoundException();
        }
        final PasswordResetToken resetToken = passwordResetTokenRepositoryPort
                .findByTokenHash(PasswordResetToken.hashToken(token))
                .orElseThrow(PasswordResetTokenNotFoundException::new);
        if (resetToken.isUsed()) {
            throw new PasswordResetTokenNotFoundException();
        }
        if (resetToken.isExpired(LocalDateTime.now(clock))) {
            throw new PasswordResetTokenExpiredException();
        }
        return resetToken;
    }
}
