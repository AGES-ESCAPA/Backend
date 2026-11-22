package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import com.escapa.backend.application.port.PasswordResetTokenRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.UserStatus;

/**
 * Pedido de recuperacao de senha. Se o e-mail existir e a conta estiver ativa,
 * emite um token e devolve os dados para o envio do e-mail. Caso contrario
 * devolve vazio — a API sempre responde igual, para nao revelar cadastros.
 */
public class RequestPasswordResetUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordResetTokenRepositoryPort passwordResetTokenRepositoryPort;
    private final Clock clock;
    private final long expirationHours;

    public RequestPasswordResetUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordResetTokenRepositoryPort passwordResetTokenRepositoryPort,
            Clock clock,
            long expirationHours
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordResetTokenRepositoryPort = passwordResetTokenRepositoryPort;
        this.clock = clock;
        this.expirationHours = expirationHours;
    }

    public Optional<PasswordResetMailCommand> execute(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        final String normalized = email.trim().toLowerCase(Locale.ROOT);
        final Optional<User> found = userRepositoryPort.findByEmail(normalized);
        if (found.isEmpty() || found.get().getStatus() != UserStatus.ACTIVE) {
            return Optional.empty();
        }

        final User user = found.get();
        final LocalDateTime now = LocalDateTime.now(clock);
        passwordResetTokenRepositoryPort.invalidateOpenTokens(user.getId(), now);

        final String rawToken = PasswordResetToken.generateToken();
        passwordResetTokenRepositoryPort.save(
                PasswordResetToken.issue(user.getId(), PasswordResetToken.hashToken(rawToken), now, expirationHours));

        return Optional.of(new PasswordResetMailCommand(user.getName(), user.getEmail(), rawToken));
    }
}
