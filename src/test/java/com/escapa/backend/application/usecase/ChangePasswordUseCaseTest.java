package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InvalidCurrentPasswordException;
import com.escapa.backend.domain.user.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangePasswordUseCaseTest {

    private final InMemoryUserRepositoryPort repository = new InMemoryUserRepositoryPort();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final ChangePasswordUseCase useCase = new ChangePasswordUseCase(repository, hasher);

    private UUID userId;

    @BeforeEach
    void setUp() {
        final User user = repository.save(
                new User("Maria Silva", "maria@email.com", hasher.hash("senha-atual"), "STUDENT"));
        userId = user.getId();
    }

    @Test
    void shouldReplaceHashWhenCurrentPasswordMatches() {
        useCase.execute(userId, "senha-atual", "nova-senha-segura");

        final String storedHash = storedHash();
        assertTrue(hasher.matches("nova-senha-segura", storedHash));
        assertFalse(hasher.matches("senha-atual", storedHash));
    }

    @Test
    void shouldRejectWrongCurrentPasswordAndKeepHash() {
        assertThrows(
                InvalidCurrentPasswordException.class,
                () -> useCase.execute(userId, "senha-errada", "nova-senha-segura")
        );

        assertTrue(hasher.matches("senha-atual", storedHash()));
    }

    @Test
    void shouldRejectNewPasswordShorterThanMinimum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(userId, "senha-atual", "1234567")
        );

        assertTrue(hasher.matches("senha-atual", storedHash()));
    }

    @Test
    void shouldAcceptNewPasswordWithExactlyMinimumLength() {
        useCase.execute(userId, "senha-atual", "12345678");

        assertTrue(hasher.matches("12345678", storedHash()));
    }

    @Test
    void shouldRejectBlankCurrentPassword() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(userId, "   ", "nova-senha-segura")
        );

        assertTrue(hasher.matches("senha-atual", storedHash()));
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        final UUID unknownId = UUID.randomUUID();

        assertThrows(
                UserNotFoundException.class,
                () -> useCase.execute(unknownId, "senha-atual", "nova-senha-segura")
        );
    }

    private String storedHash() {
        return repository.findById(userId).orElseThrow().getPasswordHash();
    }
}
