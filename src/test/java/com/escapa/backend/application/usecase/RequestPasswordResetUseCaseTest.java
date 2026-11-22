package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.PasswordResetToken;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class RequestPasswordResetUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK =
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryPasswordResetTokenRepositoryPort tokenRepository =
            new InMemoryPasswordResetTokenRepositoryPort();
    private final RequestPasswordResetUseCase useCase =
            new RequestPasswordResetUseCase(userRepository, tokenRepository, CLOCK, 2);

    @Test
    void shouldIssueTokenForActiveUser() {
        final User user = userRepository.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));

        final Optional<PasswordResetMailCommand> result = useCase.execute("Ana@escapa.com");

        assertTrue(result.isPresent());
        assertEquals("Ana", result.get().name());
        assertEquals("ana@escapa.com", result.get().email());
        assertTrue(tokenRepository.findByTokenHash(
                PasswordResetToken.hashToken(result.get().token())).isPresent());
        assertEquals(user.getId(), tokenRepository.findByTokenHash(
                PasswordResetToken.hashToken(result.get().token())).orElseThrow().getUserId());
    }

    @Test
    void shouldInvalidatePreviousOpenTokensWhenIssuingANewOne() {
        final User user = userRepository.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));
        final PasswordResetToken previous = tokenRepository.save(PasswordResetToken.issue(
                user.getId(), PasswordResetToken.hashToken("antigo"), LocalDateTime.now(CLOCK), 2));

        useCase.execute("ana@escapa.com");

        assertTrue(tokenRepository.findByTokenHash(previous.getTokenHash()).orElseThrow().isUsed());
    }

    @Test
    void shouldReturnEmptyForUnknownEmail() {
        assertTrue(useCase.execute("nobody@escapa.com").isEmpty());
    }

    @Test
    void shouldReturnEmptyForInactiveUser() {
        userRepository.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.INACTIVE, LocalDateTime.now(CLOCK)));

        assertTrue(useCase.execute("ana@escapa.com").isEmpty());
    }

    @Test
    void shouldReturnEmptyForPendingUser() {
        userRepository.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.PENDING, LocalDateTime.now(CLOCK)));

        assertTrue(useCase.execute("ana@escapa.com").isEmpty());
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("  "));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
    }
}
