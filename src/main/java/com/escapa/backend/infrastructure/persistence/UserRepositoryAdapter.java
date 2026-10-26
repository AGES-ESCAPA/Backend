package com.escapa.backend.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.escapa.backend.application.model.StudentContact;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;

@Repository
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final UserJpaRepository userJpaRepository;
    private final UsersCompanyJpaRepository usersCompanyJpaRepository;
    private final RegularUserJpaRepository regularUserJpaRepository;

    public UserRepositoryAdapter(
            UserJpaRepository userJpaRepository,
            UsersCompanyJpaRepository usersCompanyJpaRepository,
            RegularUserJpaRepository regularUserJpaRepository
    ) {
        this.userJpaRepository = userJpaRepository;
        this.usersCompanyJpaRepository = usersCompanyJpaRepository;
        this.regularUserJpaRepository = regularUserJpaRepository;
    }

        @Override
    public User save(User user) {
        final UserEntity entity;
        if (user.getId() != null) {
            entity = userJpaRepository.findById(user.getId())
                    .orElseGet(() -> UserMapper.toEntity(user));
            UserMapper.updateEntity(entity, user);
        } else {
            entity = UserMapper.toEntity(user);
        }
        final UserEntity saved = userJpaRepository.save(entity);
        return UserMapper.toDomain(saved);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userJpaRepository.findAll().stream()
                .map(UserMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(UserMapper::toDomain);
    }

    @Override
    public boolean isUserLinkedToCompany(UUID userId) {
        if (userId == null) {
            return false;
        }
        return usersCompanyJpaRepository.existsByIdUserId(userId);
    }

    @Override
    public void updatePasswordHash(UUID id, String passwordHash) {
        userJpaRepository.updatePasswordHash(id, passwordHash);
    }

    @Override
    public Optional<StudentContact> findStudentContact(UUID userId) {
        return regularUserJpaRepository.findContactByUserId(userId);
    }

    @Override
    public void updateStudentPhone(UUID userId, String phone) {
        regularUserJpaRepository.upsertPhone(userId, phone);
    }
}
