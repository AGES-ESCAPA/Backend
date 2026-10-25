package com.escapa.backend.application.usecase;

import java.util.UUID;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.user.InactiveUserException;
import com.escapa.backend.domain.user.UserStatus;

/**
 * Recusa requisicoes de usuario desativado (US-24). Usuario inexistente passa adiante, para que
 * cada rota mantenha a propria resposta de "nao encontrado".
 */
public class AuthorizeActiveUserUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public AuthorizeActiveUserUseCase(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public void execute(UUID userId) {
        userRepositoryPort.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.INACTIVE)
                .ifPresent(user -> {
                    throw new InactiveUserException();
                });
    }
}
