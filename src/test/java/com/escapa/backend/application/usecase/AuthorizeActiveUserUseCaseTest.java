package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InactiveUserException;
import com.escapa.backend.domain.user.UserStatus;

class AuthorizeActiveUserUseCaseTest {

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final AuthorizeActiveUserUseCase useCase = new AuthorizeActiveUserUseCase(userRepository);

    private User saveUser() {
        return userRepository.save(new User("Maria Silva", "maria@email.com", "hash", "STUDENT"));
    }

    @Test
    void shouldAllowActiveUser() {
        final User user = saveUser();

        assertDoesNotThrow(() -> useCase.execute(user.getId()));
    }

    @Test
    void shouldRejectInactiveUser() {
        final User user = saveUser();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        assertThrows(InactiveUserException.class, () -> useCase.execute(user.getId()));
    }

    @Test
    void shouldLetUnknownUserThroughSoTheRouteDecides() {
        assertDoesNotThrow(() -> useCase.execute(UUID.randomUUID()));
    }
}
