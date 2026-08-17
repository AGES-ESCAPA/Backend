package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.user.User;
import com.escapa.backend.domain.user.UserNotFoundException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetUserByIdUseCaseTest {

    @Test
    void shouldReturnUserWhenFound() {
        UserRepositoryPort repository = new InMemoryUserRepositoryPort();
        User saved = repository.save(User.create("Maria", "maria@email.com", "student"));
        GetUserByIdUseCase useCase = new GetUserByIdUseCase(repository);

        User found = useCase.execute(saved.getId());

        assertEquals(saved.getId(), found.getId());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        UserRepositoryPort repository = new InMemoryUserRepositoryPort();
        GetUserByIdUseCase useCase = new GetUserByIdUseCase(repository);

        assertThrows(UserNotFoundException.class, () -> useCase.execute("unknown-id"));
    }
}
