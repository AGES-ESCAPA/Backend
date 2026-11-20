package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserResponseTest {

    @Test
    void shouldDefaultMissingUserStatusToActive() {
        final User user = new User("Maria", "maria@example.com", "hash", "STUDENT");
        user.setStatus(null);

        final UserResponse response = UserResponse.from(user);

        assertEquals(UserStatus.ACTIVE.name(), response.status());
    }
}
