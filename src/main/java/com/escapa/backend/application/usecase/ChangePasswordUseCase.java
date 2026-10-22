package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InvalidCurrentPasswordException;
import com.escapa.backend.domain.user.UserNotFoundException;

import java.util.UUID;

/**
 * Troca a senha do usuario logado, exigindo a senha atual. A senha incorreta
 * vira 400, e nao 401, para o frontend nao tratar a resposta como sessao expirada.
 */
public class ChangePasswordUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public ChangePasswordUseCase(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    public void execute(UUID userId, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Current password is required");
        }
        if (newPassword == null || newPassword.length() < CreateUserUseCase.MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + CreateUserUseCase.MIN_PASSWORD_LENGTH + " characters");
        }

        final User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordHasherPort.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        userRepositoryPort.updatePasswordHash(userId, passwordHasherPort.hash(newPassword));
    }
}
