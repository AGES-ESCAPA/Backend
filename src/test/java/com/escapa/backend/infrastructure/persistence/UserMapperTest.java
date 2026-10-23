package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    @Test
    void shouldMapUserToEntityAndPreserveIdentityAndCreationTime() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2025, 1, 2, 3, 4);
        final User user = new User(id, "Maria", "maria@example.com", "hash", "STUDENT",
                "/avatar.png", UserStatus.INACTIVE, createdAt);

        final UserEntity entity = UserMapper.toEntity(user);

        assertEquals(id, entity.getId());
        assertEquals("Maria", entity.getName());
        assertEquals("maria@example.com", entity.getEmail());
        assertEquals("hash", entity.getPasswordHash());
        assertEquals("STUDENT", entity.getRole());
        assertEquals(com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.INACTIVE,
                entity.getStatus());
        assertEquals("/avatar.png", entity.getProfileAvatarUrl());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void shouldGenerateMissingIdentityAndCreationTimeAndDefaultToActive() {
        final User user = new User(null, "João", "joao@example.com", "hash", "STUDENT",
                null, null, null);

        final UserEntity entity = UserMapper.toEntity(user);

        assertNotNull(entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertEquals(com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.ACTIVE,
                entity.getStatus());
    }

    @Test
    void shouldUpdateAllMutableEntityFields() {
        final UserEntity entity = new UserEntity();
        final User user = new User(UUID.randomUUID(), "Ana", "ana@example.com", "new-hash",
                "ADMIN", "/new-avatar.png", UserStatus.INACTIVE, LocalDateTime.now());

        UserMapper.updateEntity(entity, user);

        assertEquals("Ana", entity.getName());
        assertEquals("ana@example.com", entity.getEmail());
        assertEquals("new-hash", entity.getPasswordHash());
        assertEquals("ADMIN", entity.getRole());
        assertEquals(com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.INACTIVE,
                entity.getStatus());
        assertEquals("/new-avatar.png", entity.getProfileAvatarUrl());
    }

    @Test
    void shouldMapEntityToDomainAndDefaultNullStatusToActive() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2025, 3, 4, 5, 6);
        final UserEntity entity = new UserEntity(id, "Paulo", "paulo@example.com", "hash",
                "STUDENT", createdAt);
        entity.setProfileAvatarUrl("/avatar.png");
        entity.setStatus(com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.INACTIVE);

        final User user = UserMapper.toDomain(entity);

        assertEquals(id, user.getId());
        assertEquals("Paulo", user.getName());
        assertEquals("paulo@example.com", user.getEmail());
        assertEquals("hash", user.getPasswordHash());
        assertEquals("STUDENT", user.getUserType());
        assertEquals("/avatar.png", user.getAvatarUrl());
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        assertEquals(createdAt, user.getCreatedAt());

        entity.setStatus(null);
        assertEquals(UserStatus.ACTIVE, UserMapper.toDomain(entity).getStatus());
        assertNull(UserMapper.toDomain(null));
        assertNull(UserMapper.toEntity(null));
    }
}
