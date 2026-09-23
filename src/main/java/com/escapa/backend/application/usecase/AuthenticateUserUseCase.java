package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.TokenServicePort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;

public class AuthenticateUserUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final TokenServicePort tokenServicePort;

    public AuthenticateUserUseCase(UserRepositoryPort userRepositoryPort, PasswordHasherPort passwordHasherPort, TokenServicePort tokenServicePort) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.tokenServicePort = tokenServicePort;
    }

    public String execute(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }
        
        final User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
                
        if (!passwordHasherPort.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        
        return tokenServicePort.generateToken(user);
    }
}
