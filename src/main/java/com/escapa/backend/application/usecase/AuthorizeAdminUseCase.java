package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.AdminAccessDeniedException;

import java.util.UUID;

/** Garante que o usuario identificado existe e e ADMIN, devolvendo-o para uso pelo chamador. */
public class AuthorizeAdminUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public AuthorizeAdminUseCase(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public User execute(UUID userId) {
        final User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new AdminAccessDeniedException("User not found"));
        if (!user.isAdmin()) {
            throw new AdminAccessDeniedException("Access denied: user is not ADMIN");
        }
        return user;
    }
}
