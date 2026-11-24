package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class UserMapperTest {

    @Test
    void shouldReturnNullWhenMappingNullUserOrEntity() {
        assertNull(UserMapper.toEntity(null));
        assertNull(UserMapper.toDomain(null));
    }

    @Test
    void shouldMapDomainToEntityGeneratingIdAndCreatedAtWhenMissing() {
        final User user = new User("Mariana", "mariana@email.com", "hash", UserTypes.STUDENT);
        user.setId(null);
        user.setCreatedAt(null);
        user.setAvatarUrl("http://minio/avatars/mariana.png");
        user.setStatus(UserStatus.PENDING);

        final UserEntity entity = UserMapper.toEntity(user);

        assertNotNull(entity.getId());
        assertEquals("Mariana", entity.getName());
        assertEquals("mariana@email.com", entity.getEmail());
        assertEquals("hash", entity.getPasswordHash());
        assertEquals(UserTypes.STUDENT, entity.getRole());
        assertEquals(
                com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.PENDING,
                entity.getStatus());
        assertEquals("http://minio/avatars/mariana.png", entity.getProfileAvatarUrl());
        assertNotNull(entity.getCreatedAt());
    }

    @Test
    void shouldPreserveIdAndCreatedAtWhenPresent() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final User user = new User(id, "Admin", "admin@escapa.com", "hash", UserTypes.ADMIN,
                null, UserStatus.ACTIVE, createdAt);

        final UserEntity entity = UserMapper.toEntity(user);

        assertEquals(id, entity.getId());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void shouldUpdateMutableEntityFields() {
        final UserEntity entity = new UserEntity(
                UUID.randomUUID(), "Antigo", "antigo@email.com", "old-hash", UserTypes.STUDENT,
                LocalDateTime.of(2026, 1, 1, 10, 0));
        final User user = new User(
                entity.getId(), "Novo", "novo@email.com", "new-hash", UserTypes.ADMIN,
                "http://minio/avatars/novo.png", UserStatus.INACTIVE, entity.getCreatedAt());

        UserMapper.updateEntity(entity, user);

        assertEquals("Novo", entity.getName());
        assertEquals("novo@email.com", entity.getEmail());
        assertEquals("new-hash", entity.getPasswordHash());
        assertEquals(UserTypes.ADMIN, entity.getRole());
        assertEquals(
                com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.INACTIVE,
                entity.getStatus());
        assertEquals("http://minio/avatars/novo.png", entity.getProfileAvatarUrl());
    }

    @Test
    void shouldMapEntityToDomain() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        final UserEntity entity = new UserEntity(
                id, "Empresa", "empresa@email.com", "hash", UserTypes.COMPANY,
                com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.ACTIVE,
                "http://minio/avatars/empresa.png",
                createdAt);

        final User domain = UserMapper.toDomain(entity);

        assertEquals(id, domain.getId());
        assertEquals("Empresa", domain.getName());
        assertEquals("empresa@email.com", domain.getEmail());
        assertEquals("hash", domain.getPasswordHash());
        assertEquals(UserTypes.COMPANY, domain.getUserType());
        assertEquals(UserStatus.ACTIVE, domain.getStatus());
        assertEquals("http://minio/avatars/empresa.png", domain.getAvatarUrl());
        assertEquals(createdAt, domain.getCreatedAt());
    }
}
