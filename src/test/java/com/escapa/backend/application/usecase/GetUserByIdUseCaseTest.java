package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetUserByIdUseCaseTest {

    @Test
    void shouldReturnUserWhenFound() {
        final UserRepositoryPort repository = new InMemoryUserRepositoryPort();
        final User saved = repository.save(new User("Maria Silva", "maria@email.com", "hash", "STUDENT"));
        final GetUserByIdUseCase useCase = new GetUserByIdUseCase(repository);

        final User found = useCase.execute(saved.getId());

        assertEquals(saved.getId(), found.getId());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        final UserRepositoryPort repository = new InMemoryUserRepositoryPort();
        final GetUserByIdUseCase useCase = new GetUserByIdUseCase(repository);

        assertThrows(UserNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }
}
