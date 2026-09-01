package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Test
    void shouldPersistAndRetrieveUserCreatedWithoutId() {
        final User user = new User("Maria Persist", "maria.persist@email.com", "hash", "STUDENT");

        final User saved = userRepositoryPort.save(user);

        assertNotNull(saved.getId());
        final Optional<User> found = userRepositoryPort.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("Maria Persist", found.get().getName());
        assertEquals("maria.persist@email.com", found.get().getEmail());
        assertEquals("STUDENT", found.get().getUserType());
        assertEquals("hash", found.get().getPasswordHash());
    }

    @Test
    void shouldKeepCreatedAtStableAcrossReads() {
        final User saved = userRepositoryPort.save(
                new User("Carlos Data", "carlos.data@email.com", "hash", "STUDENT"));

        final Optional<User> first = userRepositoryPort.findById(saved.getId());
        final Optional<User> second = userRepositoryPort.findById(saved.getId());

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(first.get().getCreatedAt(), second.get().getCreatedAt());
    }

    @Test
    void shouldReportWhetherEmailIsAlreadyTaken() {
        userRepositoryPort.save(new User("Joao Exists", "joao.exists@email.com", "hash", "STUDENT"));

        assertTrue(userRepositoryPort.existsByEmail("joao.exists@email.com"));
        assertFalse(userRepositoryPort.existsByEmail("desconhecido@email.com"));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        userRepositoryPort.save(new User("Ana Unique", "ana.unique@email.com", "hash", "STUDENT"));

        final User duplicate = new User("Ana Clone", "ana.unique@email.com", "hash", "TEACHER");

        assertThrows(DataIntegrityViolationException.class, () -> userRepositoryPort.save(duplicate));
    }
}
