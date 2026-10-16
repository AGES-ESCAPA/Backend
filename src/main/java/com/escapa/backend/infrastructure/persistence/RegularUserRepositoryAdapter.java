package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;

import com.escapa.backend.application.port.RegularUserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.RegistrationConflictException;
import com.escapa.backend.infrastructure.persistence.entity.RegularUserEntity;

public class RegularUserRepositoryAdapter implements RegularUserRepositoryPort {

    private final RegularUserJpaRepository regularUserJpaRepository;

    public RegularUserRepositoryAdapter(RegularUserJpaRepository regularUserJpaRepository) {
        this.regularUserJpaRepository = regularUserJpaRepository;
    }

    @Override
    public User save(User user, String cpf, String phone) {
        final UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
        final LocalDateTime createdAt = user.getCreatedAt() != null
                ? user.getCreatedAt()
                : LocalDateTime.now();
        final RegularUserEntity entity = new RegularUserEntity(
                id,
                user.getName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getUserType(),
                createdAt,
                cpf,
                phone
        );
        try {
            return UserMapper.toDomain(regularUserJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw translateConflict(exception);
        }
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return regularUserJpaRepository.existsByCpf(cpf);
    }

    private static RuntimeException translateConflict(DataIntegrityViolationException exception) {
        final String message = exception.getMostSpecificCause().getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("cpf")) {
            return new RegistrationConflictException("CPF");
        }
        if (message.contains("email")) {
            return new RegistrationConflictException("Email");
        }
        return exception;
    }
}
