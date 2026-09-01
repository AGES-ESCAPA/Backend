package com.escapa.backend.application.port;

import com.escapa.backend.domain.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    boolean existsByEmail(String email);
    List<User> findAll();
    Optional<User> findById(UUID id);
}
