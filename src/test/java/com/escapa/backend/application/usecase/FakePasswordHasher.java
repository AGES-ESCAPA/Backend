package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.PasswordHasherPort;

/** Hash previsivel para teste: evita o custo do BCrypt sem mudar o contrato da porta. */
final class FakePasswordHasher implements PasswordHasherPort {

    static final String PREFIX = "hashed:";

    @Override
    public String hash(String rawPassword) {
        return PREFIX + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return hash(rawPassword).equals(passwordHash);
    }
}
