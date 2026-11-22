package com.escapa.backend.adapters.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;

class UserResponseTest {

    @Test
    void shouldDefaultMissingUserStatusToActive() {
        final User user = new User("Maria", "maria@example.com", "hash", "STUDENT");
        user.setStatus(null);

        final UserResponse response = UserResponse.from(user);

        assertEquals(UserStatus.ACTIVE.name(), response.status());
        assertEquals("STUDENT", response.userType());
        assertNull(response.cpf());
    }

    @Test
    void shouldAcceptAResolvedProfileDistinctFromTheStoredRole() {
        final User user = new User("Luciana", "luciana@example.com", "hash", "STUDENT");

        final UserResponse response = UserResponse.from(user, "EMPLOYEE");

        assertEquals("EMPLOYEE", response.userType());
    }

    @Test
    void shouldMapRegistrationFieldsFromProfile() {
        final User user = new User("Maria", "maria@example.com", "hash", "STUDENT");
        final UserProfile profile = new UserProfile(
                "STUDENT",
                true,
                "Maria",
                "maria@example.com",
                "39011122233",
                "51991203344",
                null,
                null,
                null,
                null,
                null);

        final UserResponse response = UserResponse.from(user, profile);

        assertEquals("39011122233", response.cpf());
        assertEquals("51991203344", response.phone());
        assertEquals("STUDENT", response.userType());
    }
}
