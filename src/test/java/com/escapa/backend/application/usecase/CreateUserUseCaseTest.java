package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CreateUserUseCaseTest {

    @Test
    void shouldCreateUserWithNormalizedData() {
        UserRepositoryPort repository = new InMemoryUserRepositoryPort();
        CreateUserUseCase useCase = new CreateUserUseCase(repository);

        User user = useCase.execute(" maria ", " maria@email.com ", "student ");

        assertNotNull(user.getId());
        assertEquals("maria", user.getName());
        assertEquals("maria@email.com", user.getEmail());
        assertEquals("STUDENT", user.getRole());
    }
}
