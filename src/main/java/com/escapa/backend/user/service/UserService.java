package com.escapa.backend.user.service;

import com.escapa.backend.user.dto.CreateUserRequest;
import com.escapa.backend.user.entity.UserEntity;
import com.escapa.backend.user.exception.EmailAlreadyUsedException;
import com.escapa.backend.user.exception.UserNotFoundException;
import com.escapa.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Regras de usuário. Formato da entrada já chegou validado pelo DTO;
 * aqui ficam normalização, unicidade de email e hash da senha.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity create(CreateUserRequest request) {
        final String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        final UserEntity user = new UserEntity(
                UUID.randomUUID(),
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.userType().trim().toUpperCase(Locale.ROOT),
                LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> list() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public UserEntity getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
