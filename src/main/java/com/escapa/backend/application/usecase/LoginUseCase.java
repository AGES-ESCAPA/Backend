package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InactiveUserException;
import com.escapa.backend.domain.user.InvalidCredentialsException;
import com.escapa.backend.domain.user.UserStatus;

public class LoginUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenProviderPort tokenProviderPort;

    public LoginUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordHasherPort passwordHasherPort,
            TokenProviderPort tokenProviderPort
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenProviderPort = tokenProviderPort;
    }

    public LoginOutput execute(String email, String password) {
        final String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        if (normalizedEmail.isBlank() || password == null || password.isBlank()) {
            throw new InvalidCredentialsException();
        }

        final User user = userRepositoryPort.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasherPort.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new InactiveUserException();
        }

        final String role = user.getUserType() != null ? user.getUserType().trim().toUpperCase() : "STUDENT";
        final String profile = resolveProfile(user, role);

        final String token = tokenProviderPort.generateToken(user.getId(), user.getEmail(), role, profile);
        return new LoginOutput(token, "Bearer", tokenProviderPort.getExpirationSeconds(), user, profile);
    }

    private String resolveProfile(User user, String role) {
        if ("ADMIN".equals(role)) {
            return "ADMIN";
        }
        if ("COMPANY".equals(role)) {
            return "COMPANY";
        }
        final boolean isLinkedToCompany = user.getId() != null
                && userRepositoryPort.isUserLinkedToCompany(user.getId());
        return isLinkedToCompany ? "EMPLOYEE" : "STUDENT";
    }
}

