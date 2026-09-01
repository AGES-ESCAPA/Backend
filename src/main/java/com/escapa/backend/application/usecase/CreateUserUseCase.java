package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class CreateUserUseCase {
    private final UserRepositoryPort userRepositoryPort;

    public CreateUserUseCase(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    public User execute(String email, String password, String userType) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }
        final String normalizedEmail = email.trim().toLowerCase();
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (userType == null || userType.trim().isBlank()) {
            throw new IllegalArgumentException("User type is required");
        }
        final String normalizedUserType = userType.trim().toUpperCase();

        if (userRepositoryPort.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("User already exists");
        }

        final User user = new User(null, normalizedEmail, password, normalizedUserType, LocalDateTime.now(), new ArrayList<>());
        return userRepositoryPort.save(user);
    }
}
