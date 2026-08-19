package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.user.User;
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
        User user = User.create("Maria Souza", "maria.persist@email.com", "student");

        userRepositoryPort.save(user);

        Optional<User> found = userRepositoryPort.findById(user.getId());
        assertTrue(found.isPresent());
        assertEquals(user.getId(), found.get().getId());
        assertEquals("Maria Souza", found.get().getName());
        assertEquals("maria.persist@email.com", found.get().getEmail());
        assertEquals("STUDENT", found.get().getRole());
    }

    @Test
    void shouldReportWhetherEmailIsAlreadyTaken() {
        User user = User.create("Joao Lima", "joao.exists@email.com", "student");

        userRepositoryPort.save(user);

        assertTrue(userRepositoryPort.existsByEmail("joao.exists@email.com"));
        assertFalse(userRepositoryPort.existsByEmail("desconhecido@email.com"));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        User first = User.create("Ana Costa", "ana.unique@email.com", "student");
        userRepositoryPort.save(first);

        User duplicate = User.create("Outra Ana", "ana.unique@email.com", "teacher");

        assertThrows(DataIntegrityViolationException.class, () -> userRepositoryPort.save(duplicate));
    }
}
