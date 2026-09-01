package com.escapa.backend.application.port;

import com.escapa.backend.domain.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    boolean existsByEmail(String email);
    List<User> findAll();
    Optional<User> findById(String id);
}
