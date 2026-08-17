package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class InMemoryUserRepositoryPort implements UserRepositoryPort {
    private final List<User> users = new ArrayList<>();

    @Override
    public User save(User user) {
        users.add(user);
        return user;
    }

    @Override
    public boolean existsByEmail(String email) {
        return users.stream().anyMatch(user -> user.getEmail().equals(email));
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(users);
    }

    @Override
    public Optional<User> findById(String id) {
        return users.stream().filter(user -> user.getId().equals(id)).findFirst();
    }
}
