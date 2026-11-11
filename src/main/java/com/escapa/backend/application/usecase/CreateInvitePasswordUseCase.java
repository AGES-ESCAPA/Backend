package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;

import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

public class CreateInvitePasswordUseCase {

    private static final String EMPLOYEE_PROFILE = "EMPLOYEE";

    private final AccountInviteValidator accountInviteValidator;
    private final AccountInviteRepositoryPort accountInviteRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenProviderPort tokenProviderPort;
    private final Clock clock;

    public CreateInvitePasswordUseCase(
            AccountInviteRepositoryPort accountInviteRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            PasswordHasherPort passwordHasherPort,
            TokenProviderPort tokenProviderPort,
            Clock clock
    ) {
        this.accountInviteValidator = new AccountInviteValidator(accountInviteRepositoryPort, clock);
        this.accountInviteRepositoryPort = accountInviteRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenProviderPort = tokenProviderPort;
        this.clock = clock;
    }

    public LoginOutput execute(String token, String password) {
        if (password == null || password.length() < CreateUserUseCase.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + CreateUserUseCase.MIN_PASSWORD_LENGTH + " characters");
        }
        final AccountInvite invite = accountInviteValidator.requireOpenInvite(token);
        final User user = userRepositoryPort.findById(invite.getUserId())
                .orElseThrow(() -> new UserNotFoundException(invite.getUserId()));

        user.setPasswordHash(passwordHasherPort.hash(password));
        user.setStatus(UserStatus.ACTIVE);
        final User activated = userRepositoryPort.save(user);
        invite.markAsUsed(LocalDateTime.now(clock));
        accountInviteRepositoryPort.save(invite);

        final String role = activated.getUserType() != null
                ? activated.getUserType().trim().toUpperCase()
                : UserTypes.STUDENT;
        final String accessToken = tokenProviderPort.generateToken(
                activated.getId(), activated.getEmail(), role, EMPLOYEE_PROFILE);
        return new LoginOutput(
                accessToken, "Bearer", tokenProviderPort.getExpirationSeconds(), activated, EMPLOYEE_PROFILE);
    }
}
