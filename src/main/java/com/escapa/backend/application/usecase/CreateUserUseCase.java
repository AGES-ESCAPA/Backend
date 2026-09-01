package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class CreateUserUseCase {
    public static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public CreateUserUseCase(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    public User execute(String name, String email, String password, String userType) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }
        final String normalizedEmail = email.trim().toLowerCase();
        if (normalizedEmail.isBlank() || !normalizedEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (userType == null || userType.trim().isBlank()) {
            throw new IllegalArgumentException("User type is required");
        }
        final String normalizedUserType = userType.trim().toUpperCase();

        if (userRepositoryPort.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("User already exists");
        }

        final User user = new User(
                null,
                name.trim(),
                normalizedEmail,
                passwordHasherPort.hash(password),
                normalizedUserType,
                LocalDateTime.now(),
                new ArrayList<>()
        );
        return userRepositoryPort.save(user);
    }
}
