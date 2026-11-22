package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import com.escapa.backend.application.port.EmailChangeTokenRepositoryPort;
import com.escapa.backend.application.port.UserProfileChangeLogRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.domain.user.EmailChangeTokenNotFoundException;
import com.escapa.backend.domain.user.UserNotFoundException;

/**
 * Confirma a troca de e-mail com o token do link. Só então atualiza {@code users.email}
 * e registra o histórico.
 */
public class ConfirmEmailChangeUseCase {

    private final EmailChangeTokenValidator emailChangeTokenValidator;
    private final EmailChangeTokenRepositoryPort emailChangeTokenRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final UserProfileChangeLogRepositoryPort changeLogRepositoryPort;
    private final Clock clock;

    public ConfirmEmailChangeUseCase(
            EmailChangeTokenRepositoryPort emailChangeTokenRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            UserProfileChangeLogRepositoryPort changeLogRepositoryPort,
            Clock clock
    ) {
        this.emailChangeTokenValidator = new EmailChangeTokenValidator(emailChangeTokenRepositoryPort, clock);
        this.emailChangeTokenRepositoryPort = emailChangeTokenRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.changeLogRepositoryPort = changeLogRepositoryPort;
        this.clock = clock;
    }

    public void execute(String token) {
        final EmailChangeToken changeToken = emailChangeTokenValidator.requireOpenToken(token);
        if (!emailChangeTokenRepositoryPort.markAsUsed(changeToken.getId(), LocalDateTime.now(clock))) {
            throw new EmailChangeTokenNotFoundException();
        }

        final User user = userRepositoryPort.findById(changeToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(changeToken.getUserId()));

        userRepositoryPort.findByEmail(changeToken.getNewEmail())
                .filter(owner -> !owner.getId().equals(user.getId()))
                .ifPresent(owner -> {
                    throw new DuplicateRegistrationException("email");
                });

        final String oldEmail = user.getEmail();
        user.setEmail(changeToken.getNewEmail());
        userRepositoryPort.save(user);

        changeLogRepositoryPort.append(
                user.getId(),
                changeToken.getRequestedBy(),
                "email",
                oldEmail,
                changeToken.getNewEmail());

        emailChangeTokenRepositoryPort.invalidateOpenTokens(user.getId(), LocalDateTime.now(clock));
    }
}
