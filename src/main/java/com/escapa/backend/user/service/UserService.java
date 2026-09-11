package com.escapa.backend.user.service;

import com.escapa.backend.user.dto.CreateUserRequest;
import com.escapa.backend.user.entity.AdminEntity;
import com.escapa.backend.user.entity.RegularUserEntity;
import com.escapa.backend.user.entity.UserEntity;
import com.escapa.backend.user.entity.UserRole;
import com.escapa.backend.user.exception.EmailAlreadyUsedException;
import com.escapa.backend.user.exception.UserNotFoundException;
import com.escapa.backend.user.exception.UserTypeNotSupportedException;
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
 * aqui ficam normalização, unicidade de email, hash da senha e a escolha da
 * subclasse JPA que corresponde ao papel (herança JOINED: {@code users} + tabela filha).
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
        final UserRole role = UserRole.valueOf(request.userType().trim().toUpperCase(Locale.ROOT));
        if (role == UserRole.COMPANY) {
            throw new UserTypeNotSupportedException(role);
        }
        final String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        final UserEntity user = newUser(role, request.name().trim(), email, passwordEncoder.encode(request.password()));
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

    /**
     * Um papel, uma subclasse. Gravar um {@code UserEntity} puro deixaria o usuário sem linha
     * na tabela filha, e um "admin" assim não poderia ser instrutor (FK aponta para {@code admins}).
     */
    private static UserEntity newUser(UserRole role, String name, String email, String passwordHash) {
        final UUID id = UUID.randomUUID();
        final LocalDateTime now = LocalDateTime.now();
        return switch (role) {
            case STUDENT -> new RegularUserEntity(id, name, email, passwordHash, role, now, null, null);
            case ADMIN -> new AdminEntity(id, name, email, passwordHash, role, now, null);
            case COMPANY -> throw new UserTypeNotSupportedException(role);
        };
    }
}
