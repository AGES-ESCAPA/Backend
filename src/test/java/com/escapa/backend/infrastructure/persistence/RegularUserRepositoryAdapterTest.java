package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.escapa.backend.application.port.RegularUserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.infrastructure.persistence.entity.RegularUserEntity;

class RegularUserRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private RegularUserRepositoryPort regularUserRepositoryPort;

    @Autowired
    private RegularUserJpaRepository regularUserJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void shouldPersistUserAndRegularUserProfile() {
        final String suffix = UUID.randomUUID().toString();
        final User user = new User("Student Persist", suffix + "@email.com", "hash", "STUDENT");

        final User saved = regularUserRepositoryPort.save(user, suffix, "51999990000");

        assertNotNull(saved.getId());
        assertTrue(userJpaRepository.existsById(saved.getId()));
        assertTrue(regularUserJpaRepository.existsById(saved.getId()));
        assertTrue(regularUserRepositoryPort.existsByCpf(suffix));
        final RegularUserEntity profile = regularUserJpaRepository.findById(saved.getId()).orElseThrow();
        assertEquals("51999990000", profile.getPhone());
    }

    @Test
    void shouldPreserveProvidedUserIdAndCreationTime() {
        final UUID id = UUID.randomUUID();
        final LocalDateTime createdAt = LocalDateTime.of(2026, 8, 15, 10, 30);
        final User user = new User("Known Student", id + "@email.com", "hash", "STUDENT");
        user.setId(id);
        user.setCreatedAt(createdAt);

        final User saved = regularUserRepositoryPort.save(user, "cpf-" + id, "51999990000");

        assertEquals(id, saved.getId());
        assertEquals(createdAt, saved.getCreatedAt());
    }

    @Test
    void shouldIdentifyDuplicateEmailContainingCpfAndNotSaveAnotherProfile() {
        final String suffix = UUID.randomUUID().toString();
        final String email = "cpf-" + suffix + "@exemplo.com";
        regularUserRepositoryPort.save(
                new User("First Student", email, "hash", "STUDENT"), "first-" + suffix, null);
        final String secondCpf = "second-" + suffix;

        final DuplicateRegistrationException exception = assertThrows(
                DuplicateRegistrationException.class,
                () -> regularUserRepositoryPort.save(
                        new User("Second Student", email, "hash", "STUDENT"), secondCpf, null));

        assertEquals("email already registered", exception.getMessage());
        assertFalse(regularUserRepositoryPort.existsByCpf(secondCpf));
    }

    @Test
    void shouldRollbackUserWhenRegularUserProfileFails() {
        final String suffix = UUID.randomUUID().toString();
        final String cpf = "duplicate-" + suffix;
        regularUserRepositoryPort.save(
                new User("First Student", "first-" + suffix + "@email.com", "hash", "STUDENT"),
                cpf,
                null
        );
        final String orphanEmail = "orphan-" + suffix + "@email.com";

        final DuplicateRegistrationException exception = assertThrows(
                DuplicateRegistrationException.class,
                () -> regularUserRepositoryPort.save(
                        new User("Second Student", orphanEmail, "hash", "STUDENT"), cpf, null)
        );

        assertEquals("cpf already registered", exception.getMessage());
        assertFalse(userJpaRepository.existsByEmail(orphanEmail));
    }
}
