package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.PasswordResetTokenRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.PasswordResetTokenNotFoundException;
import com.escapa.backend.domain.user.UserNotFoundException;

/**
 * Redefine a senha com o token do e-mail. Idempotente sob concorrencia: so a
 * primeira requisicao marca o token como usado e grava a senha.
 */
public class ResetPasswordUseCase {

    private final PasswordResetTokenValidator passwordResetTokenValidator;
    private final PasswordResetTokenRepositoryPort passwordResetTokenRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final Clock clock;

    public ResetPasswordUseCase(
            PasswordResetTokenRepositoryPort passwordResetTokenRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            PasswordHasherPort passwordHasherPort,
            Clock clock
    ) {
        this.passwordResetTokenValidator = new PasswordResetTokenValidator(passwordResetTokenRepositoryPort, clock);
        this.passwordResetTokenRepositoryPort = passwordResetTokenRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.clock = clock;
    }

    public void execute(String token, String password) {
        if (password == null || password.length() < CreateUserUseCase.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + CreateUserUseCase.MIN_PASSWORD_LENGTH + " characters");
        }

        final PasswordResetToken resetToken = passwordResetTokenValidator.requireOpenToken(token);
        if (!passwordResetTokenRepositoryPort.markAsUsed(resetToken.getId(), LocalDateTime.now(clock))) {
            throw new PasswordResetTokenNotFoundException();
        }

        if (userRepositoryPort.findById(resetToken.getUserId()).isEmpty()) {
            throw new UserNotFoundException(resetToken.getUserId());
        }

        userRepositoryPort.updatePasswordHash(resetToken.getUserId(), passwordHasherPort.hash(password));
        passwordResetTokenRepositoryPort.invalidateOpenTokens(resetToken.getUserId(), LocalDateTime.now(clock));
    }
}
