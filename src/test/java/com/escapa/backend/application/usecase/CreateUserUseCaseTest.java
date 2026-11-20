package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateUserUseCaseTest {

    private final UserRepositoryPort repository = new InMemoryUserRepositoryPort();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final CreateUserUseCase useCase = new CreateUserUseCase(repository, hasher);

    @Test
    void shouldCreateUserWithNormalizedData() {
        final User user = useCase.execute(" Maria Silva ", " maria@email.com ", "password123", "student ");

        assertNotNull(user.getId());
        assertEquals("Maria Silva", user.getName());
        assertEquals("maria@email.com", user.getEmail());
        assertEquals("STUDENT", user.getUserType());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void shouldStoreHashedPasswordInsteadOfRawPassword() {
        final User user = useCase.execute("Maria Silva", "maria@email.com", "password123", "STUDENT");

        assertNotEquals("password123", user.getPasswordHash());
        assertTrue(hasher.matches("password123", user.getPasswordHash()));
    }

    @Test
    void shouldRejectPasswordShorterThanMinimum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute("Maria Silva", "maria@email.com", "curta", "STUDENT")
        );
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute("   ", "maria@email.com", "password123", "STUDENT")
        );
    }

    @Test
    void shouldRejectMissingAndMalformedRequiredFields() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(null, "maria@email.com", "password123", "STUDENT"));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute("Maria", null, "password123", "STUDENT"));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute("Maria", "   ", "password123", "STUDENT"));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute("Maria", "invalid-email", "password123", "STUDENT"));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute("Maria", "maria@email.com", null, "STUDENT"));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute("Maria", "maria@email.com", "password123", null));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute("Maria", "maria@email.com", "password123", "   "));
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() {
        useCase.execute("Maria Silva", "maria@email.com", "password123", "STUDENT");

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute("Outra Maria", "MARIA@email.com", "password123", "TEACHER")
        );
    }
}
