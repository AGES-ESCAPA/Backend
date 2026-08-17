package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.user.User;

public class CreateUserUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public CreateUserUseCase(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public User execute(String name, String email, String role) {
        final User user = User.create(name, email, role);

        if (userRepositoryPort.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("User already exists");
        }

        return userRepositoryPort.save(user);
    }
}
