package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.escapa.backend.application.port.UserProfileChangeLogRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.UserProfileChangeLogEntity;

@Repository
public class UserProfileChangeLogRepositoryAdapter implements UserProfileChangeLogRepositoryPort {

    private final UserProfileChangeLogJpaRepository userProfileChangeLogJpaRepository;

    public UserProfileChangeLogRepositoryAdapter(
            UserProfileChangeLogJpaRepository userProfileChangeLogJpaRepository) {
        this.userProfileChangeLogJpaRepository = userProfileChangeLogJpaRepository;
    }

    @Override
    public void append(UUID userId, UUID changedBy, String fieldName, String oldValue, String newValue) {
        userProfileChangeLogJpaRepository.save(new UserProfileChangeLogEntity(
                UUID.randomUUID(),
                userId,
                changedBy,
                fieldName,
                oldValue,
                newValue,
                LocalDateTime.now()));
    }
}
