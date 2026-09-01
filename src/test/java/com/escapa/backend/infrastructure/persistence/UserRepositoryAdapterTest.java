package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Test
    void shouldPersistAndRetrieveUser() {
        final User user = new User(101, "maria.persist@email.com", "pass", "STUDENT", null);

        final User saved = userRepositoryPort.save(user);

        final Optional<User> found = userRepositoryPort.findById(String.valueOf(saved.getId()));
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("maria.persist@email.com", found.get().getEmail());
        assertEquals("STUDENT", found.get().getUserType());
    }

    @Test
    void shouldReportWhetherEmailIsAlreadyTaken() {
        final User user = new User(102, "joao.exists@email.com", "pass", "STUDENT", null);

        userRepositoryPort.save(user);

        assertTrue(userRepositoryPort.existsByEmail("joao.exists@email.com"));
        assertFalse(userRepositoryPort.existsByEmail("desconhecido@email.com"));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        final User first = new User(103, "ana.unique@email.com", "pass", "STUDENT", null);
        userRepositoryPort.save(first);

        final User duplicate = new User(104, "ana.unique@email.com", "pass", "TEACHER", null);

        assertThrows(DataIntegrityViolationException.class, () -> userRepositoryPort.save(duplicate));
    }
}
