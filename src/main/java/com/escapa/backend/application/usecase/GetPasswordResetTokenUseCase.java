package com.escapa.backend.application.usecase;

import java.time.Clock;

import com.escapa.backend.application.model.PasswordResetTokenDetails;
import com.escapa.backend.application.port.PasswordResetTokenRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.UserNotFoundException;

/** Valida o token do link de recuperacao e devolve o e-mail da conta. */
public class GetPasswordResetTokenUseCase {

    private final PasswordResetTokenValidator passwordResetTokenValidator;
    private final UserRepositoryPort userRepositoryPort;

    public GetPasswordResetTokenUseCase(
            PasswordResetTokenRepositoryPort passwordResetTokenRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            Clock clock
    ) {
        this.passwordResetTokenValidator = new PasswordResetTokenValidator(passwordResetTokenRepositoryPort, clock);
        this.userRepositoryPort = userRepositoryPort;
    }

    public PasswordResetTokenDetails execute(String token) {
        final PasswordResetToken resetToken = passwordResetTokenValidator.requireOpenToken(token);
        final User user = userRepositoryPort.findById(resetToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(resetToken.getUserId()));
        return new PasswordResetTokenDetails(user.getEmail());
    }
}
