package com.escapa.backend.application.usecase;

import java.time.Clock;

import com.escapa.backend.application.model.EmailChangeTokenDetails;
import com.escapa.backend.application.port.EmailChangeTokenRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.domain.user.UserNotFoundException;

/** Valida o token do link e devolve os dados para a página de confirmação. */
public class GetEmailChangeTokenUseCase {

    private final EmailChangeTokenValidator emailChangeTokenValidator;
    private final UserRepositoryPort userRepositoryPort;

    public GetEmailChangeTokenUseCase(
            EmailChangeTokenRepositoryPort emailChangeTokenRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            Clock clock
    ) {
        this.emailChangeTokenValidator = new EmailChangeTokenValidator(emailChangeTokenRepositoryPort, clock);
        this.userRepositoryPort = userRepositoryPort;
    }

    public EmailChangeTokenDetails execute(String token) {
        final EmailChangeToken changeToken = emailChangeTokenValidator.requireOpenToken(token);
        final User user = userRepositoryPort.findById(changeToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException(changeToken.getUserId()));
        return new EmailChangeTokenDetails(user.getName(), user.getEmail(), changeToken.getNewEmail());
    }
}
