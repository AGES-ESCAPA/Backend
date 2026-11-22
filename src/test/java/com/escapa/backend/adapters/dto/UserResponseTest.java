package com.escapa.backend.adapters.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
    }

    @Test
    void shouldAcceptAResolvedProfileDistinctFromTheStoredRole() {
        final User user = new User("Luciana", "luciana@example.com", "hash", "STUDENT");

        final UserResponse response = UserResponse.from(user, "EMPLOYEE");

        assertEquals("EMPLOYEE", response.userType());
    }
}
