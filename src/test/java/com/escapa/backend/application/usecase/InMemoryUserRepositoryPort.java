package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class InMemoryUserRepositoryPort implements UserRepositoryPort {
    private final List<User> users = new ArrayList<>();

    @Override
    public User save(User user) {
        // Espelha o adapter real: id nulo vira um UUID gerado na persistencia.
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        users.removeIf(u -> user.getId().equals(u.getId()));
        users.add(user);
        return user;
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.stream().anyMatch(user -> email.equalsIgnoreCase(user.getEmail()));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(users);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return users.stream().filter(user -> id.equals(user.getId())).findFirst();
    }
}
