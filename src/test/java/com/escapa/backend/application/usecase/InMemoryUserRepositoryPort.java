package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class InMemoryUserRepositoryPort implements UserRepositoryPort {
    private final List<User> users = new ArrayList<>();
    private int sequence = 1;

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(sequence++);
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
    public Optional<User> findById(String id) {
        try {
            final Integer intId = Integer.valueOf(id);
            return users.stream().filter(user -> intId.equals(user.getId())).findFirst();
        } catch (final NumberFormatException e) {
            return Optional.empty();
        }
    }
}
