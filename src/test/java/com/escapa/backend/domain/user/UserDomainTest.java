package com.escapa.backend.domain.user;

import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
                UserStatus.INACTIVE,
                LocalDateTime.now()
        );

        assertEquals(id, user.getId());
        assertEquals(UserStatus.INACTIVE, user.getStatus());
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

