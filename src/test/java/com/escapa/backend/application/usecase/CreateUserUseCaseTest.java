package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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

    @ParameterizedTest(name = "rejects invalid user input: {4}")
    @MethodSource("invalidInputs")
    void shouldRejectInvalidInput(String name, String email, String password, String userType, String message) {
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(name, email, password, userType));

        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() {
        useCase.execute("Maria Silva", "maria@email.com", "password123", "STUDENT");

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute("Outra Maria", "MARIA@email.com", "password123", "TEACHER")
        );
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(null, "maria@email.com", "password123", "STUDENT", "Name is required"),
                Arguments.of("Maria", null, "password123", "STUDENT", "Email is required"),
                Arguments.of("Maria", "  ", "password123", "STUDENT", "Invalid email"),
                Arguments.of("Maria", "not-an-email", "password123", "STUDENT", "Invalid email"),
                Arguments.of("Maria", "maria@email.com", null, "STUDENT", "Password must be at least 8 characters"),
                Arguments.of("Maria", "maria@email.com", "password123", null, "User type is required"),
                Arguments.of("Maria", "maria@email.com", "password123", " ", "User type is required"));
    }
}
