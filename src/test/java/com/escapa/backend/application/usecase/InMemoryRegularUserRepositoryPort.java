package com.escapa.backend.application.usecase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.escapa.backend.application.port.RegularUserRepositoryPort;
import com.escapa.backend.domain.entity.User;

public final class InMemoryRegularUserRepositoryPort implements RegularUserRepositoryPort {

    private final Map<String, User> usersByCpf = new HashMap<>();
    private final Map<UUID, String> phonesByUserId = new HashMap<>();

    @Override
    public User save(User user, String cpf, String phone) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
        }
        usersByCpf.put(cpf, user);
        phonesByUserId.put(user.getId(), phone);
        return user;
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return usersByCpf.containsKey(cpf);
    }

    User findByCpf(String cpf) {
        return usersByCpf.get(cpf);
    }

    String findPhoneByUserId(UUID userId) {
        return phonesByUserId.get(userId);
    }
}
