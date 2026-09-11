package com.escapa.backend.user.repository;

import com.escapa.backend.common.JpaIntegrationTest;
import com.escapa.backend.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryTest extends JpaIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistAndReadBackAllColumns() {
        final UserEntity saved = userRepository.saveAndFlush(newUser("maria.persist@email.com"));

        final Optional<UserEntity> found = userRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Maria Persist", found.get().getName());
        assertEquals("maria.persist@email.com", found.get().getEmail());
        assertEquals("STUDENT", found.get().getRole());
        assertEquals("hash", found.get().getPasswordHash());
        assertEquals(saved.getCreatedAt(), found.get().getCreatedAt());
    }

    @Test
    void shouldReportWhetherEmailIsAlreadyTaken() {
        userRepository.saveAndFlush(newUser("joao.exists@email.com"));

        assertTrue(userRepository.existsByEmail("joao.exists@email.com"));
        assertFalse(userRepository.existsByEmail("desconhecido@email.com"));
    }

    @Test
    void databaseShouldRejectDuplicateEmailAsLastLineOfDefense() {
        userRepository.saveAndFlush(newUser("ana.unique@email.com"));

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(newUser("ana.unique@email.com")));
    }

    private static UserEntity newUser(String email) {
        return new UserEntity(UUID.randomUUID(), "Maria Persist", email, "hash", "STUDENT", LocalDateTime.now());
    }
}
