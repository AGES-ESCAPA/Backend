package com.escapa.backend.application.usecase;

import java.util.Optional;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.TokenProviderPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InactiveUserException;
import com.escapa.backend.domain.user.InvalidCredentialsException;
import com.escapa.backend.domain.user.UserStatus;

public class LoginUseCase {

    private static final String DUMMY_PASSWORD = "dummy-password-for-timing";

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenProviderPort tokenProviderPort;
    /**
     * Hash gerado com o mesmo hasher dos usuários reais. Quando o e-mail não existe, a senha
     * é conferida contra ele para que a resposta leve o mesmo tempo do caso "senha errada"
     * e o tempo de resposta não revele quais e-mails estão cadastrados.
     */
    private final String dummyPasswordHash;

    public LoginUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordHasherPort passwordHasherPort,
            TokenProviderPort tokenProviderPort
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenProviderPort = tokenProviderPort;
        this.dummyPasswordHash = passwordHasherPort.hash(DUMMY_PASSWORD);
    }

    public LoginOutput execute(String email, String password) {
        final String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        if (normalizedEmail.isBlank() || password == null || password.isBlank()) {
            throw new InvalidCredentialsException();
        }

        final Optional<User> foundUser = userRepositoryPort.findByEmail(normalizedEmail);
        if (foundUser.isEmpty()) {
            passwordHasherPort.matches(password, dummyPasswordHash);
            throw new InvalidCredentialsException();
        }
        final User user = foundUser.get();

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

