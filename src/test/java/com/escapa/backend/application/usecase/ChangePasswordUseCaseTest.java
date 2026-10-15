package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.InvalidCurrentPasswordException;
import com.escapa.backend.domain.user.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangePasswordUseCaseTest {

    private static final String CURRENT_PASSWORD = "senha-atual";
    private static final String NEW_PASSWORD = "nova-senha-segura";

    private final InMemoryUserRepositoryPort repository = new InMemoryUserRepositoryPort();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final ChangePasswordUseCase useCase = new ChangePasswordUseCase(repository, hasher);

    private User user;

    @BeforeEach
    void setUp() {
        user = repository.save(new User("Maria Silva", "maria@email.com", hasher.hash(CURRENT_PASSWORD), "STUDENT"));
    }

    @Test
    void shouldReplaceHashWhenCurrentPasswordMatches() {
        useCase.execute(user.getId(), CURRENT_PASSWORD, NEW_PASSWORD);

        final String storedHash = repository.findById(user.getId()).orElseThrow().getPasswordHash();
        assertTrue(hasher.matches(NEW_PASSWORD, storedHash));
        assertFalse(hasher.matches(CURRENT_PASSWORD, storedHash));
    }

    @Test
    void shouldRejectWrongCurrentPasswordAndKeepHash() {
        final String originalHash = user.getPasswordHash();

        assertThrows(InvalidCurrentPasswordException.class,
                () -> useCase.execute(user.getId(), "senha-errada", NEW_PASSWORD));

        assertEquals(originalHash, repository.findById(user.getId()).orElseThrow().getPasswordHash());
    }

    @Test
    void shouldRejectNewPasswordShorterThanMinimum() {
        final String shortPassword = "a".repeat(CreateUserUseCase.MIN_PASSWORD_LENGTH - 1);
        final String originalHash = user.getPasswordHash();

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(user.getId(), CURRENT_PASSWORD, shortPassword));

        assertEquals("Password must be at least 8 characters", ex.getMessage());
        assertEquals(originalHash, repository.findById(user.getId()).orElseThrow().getPasswordHash());
    }

    @Test
    void shouldAcceptNewPasswordWithExactlyMinimumLength() {
        final String minimumPassword = "a".repeat(CreateUserUseCase.MIN_PASSWORD_LENGTH);

        useCase.execute(user.getId(), CURRENT_PASSWORD, minimumPassword);

        assertTrue(hasher.matches(minimumPassword, repository.findById(user.getId()).orElseThrow().getPasswordHash()));
    }

    @Test
    void shouldRejectBlankCurrentPassword() {
        final String originalHash = user.getPasswordHash();

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(user.getId(), " ", NEW_PASSWORD));

        assertEquals(originalHash, repository.findById(user.getId()).orElseThrow().getPasswordHash());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        assertThrows(UserNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), CURRENT_PASSWORD, NEW_PASSWORD));
    }
}
