package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CreateUserUseCaseTest {

    @Test
    void shouldCreateUserWithNormalizedData() {
        final UserRepositoryPort repository = new InMemoryUserRepositoryPort();
        final CreateUserUseCase useCase = new CreateUserUseCase(repository);

        final User user = useCase.execute(" maria@email.com ", "password123", "student ");

        assertNotNull(user.getId());
        assertEquals("maria@email.com", user.getEmail());
        assertEquals("STUDENT", user.getUserType());
    }
}
