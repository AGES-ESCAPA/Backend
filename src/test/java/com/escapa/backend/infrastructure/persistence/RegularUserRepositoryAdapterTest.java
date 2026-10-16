package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.escapa.backend.application.port.RegularUserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.RegistrationConflictException;
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
    void shouldIdentifyDuplicateEmailContainingCpfAndNotSaveAnotherProfile() {
        final String suffix = UUID.randomUUID().toString();
        final String email = "cpf-" + suffix + "@exemplo.com";
        regularUserRepositoryPort.save(
                new User("First Student", email, "hash", "STUDENT"), "first-" + suffix, null);
        final String secondCpf = "second-" + suffix;

        final RegistrationConflictException exception = assertThrows(
                RegistrationConflictException.class,
                () -> regularUserRepositoryPort.save(
                        new User("Second Student", email, "hash", "STUDENT"), secondCpf, null));

        assertEquals("Email already registered", exception.getMessage());
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

        final RegistrationConflictException exception = assertThrows(
                RegistrationConflictException.class,
                () -> regularUserRepositoryPort.save(
                        new User("Second Student", orphanEmail, "hash", "STUDENT"), cpf, null)
        );

        assertEquals("CPF already registered", exception.getMessage());
        assertFalse(userJpaRepository.existsByEmail(orphanEmail));
    }
}
