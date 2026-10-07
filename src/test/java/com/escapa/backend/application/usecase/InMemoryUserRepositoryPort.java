package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class InMemoryUserRepositoryPort implements UserRepositoryPort {
    private final List<User> users = new ArrayList<>();
    private final Set<UUID> companyLinkedUserIds = new HashSet<>();

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

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return users.stream()
                .filter(user -> email.equalsIgnoreCase(user.getEmail()))
                .findFirst();
    }

    @Override
    public boolean isUserLinkedToCompany(UUID userId) {
        if (userId == null) {
            return false;
        }
        return companyLinkedUserIds.contains(userId);
    }

    void linkUserToCompany(UUID userId) {
        if (userId != null) {
            companyLinkedUserIds.add(userId);
        }
    }
}
