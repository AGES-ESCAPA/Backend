package com.escapa.backend.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.User;

class UserDomainTest {

    @Test
    void shouldCreateUserWithDefaultActiveStatus() {
        final User user = new User("Maria", "maria@email.com", "hash123", "STUDENT");

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals("Maria", user.getName());
        assertEquals("maria@email.com", user.getEmail());
    }

    @Test
    void shouldCreateUserWithExplicitInactiveStatus() {
        final UUID id = UUID.randomUUID();
        final User user = new User(
                id,
                "Carla",
                "carla@email.com",
                "hash123",
                "STUDENT",
                null,
                UserStatus.INACTIVE,
                LocalDateTime.now()
        );

        assertEquals(id, user.getId());
        assertEquals(UserStatus.INACTIVE, user.getStatus());
    }

    @Test
    void shouldDetectAdminIgnoringCase() {
        assertTrue(new User("Admin", "admin@escapa.com", "hash", "ADMIN").isAdmin());
        assertTrue(new User("Admin", "admin@escapa.com", "hash", "admin").isAdmin());
        assertFalse(new User("Aluno", "aluno@email.com", "hash", "STUDENT").isAdmin());
    }

    @Test
    void shouldDefaultNullStatusToActiveInFullConstructor() {
        final User user = new User(
                UUID.randomUUID(),
                "Sem Status",
                "sem@email.com",
                "hash",
                "STUDENT",
                null,
                null,
                LocalDateTime.of(2026, 6, 15, 10, 0));

        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void shouldInstantiateDomainExceptionsWithDefaultAndCustomMessages() {
        final InvalidCredentialsException defaultInvalid = new InvalidCredentialsException();
        final InvalidCredentialsException customInvalid = new InvalidCredentialsException("Custom message");
        final InactiveUserException defaultInactive = new InactiveUserException();
        final InactiveUserException customInactive = new InactiveUserException("Custom inactive");

        assertEquals("Invalid email or password", defaultInvalid.getMessage());
        assertEquals("Custom message", customInvalid.getMessage());
        assertEquals("User account is inactive", defaultInactive.getMessage());
        assertEquals("Custom inactive", customInactive.getMessage());
    }
}

