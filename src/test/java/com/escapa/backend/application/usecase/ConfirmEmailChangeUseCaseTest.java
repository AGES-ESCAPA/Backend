package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.domain.user.EmailChangeTokenNotFoundException;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class ConfirmEmailChangeUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK =
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private InMemoryUserRepositoryPort users;
    private InMemoryEmailChangeTokenRepositoryPort tokens;
    private InMemoryUserProfileChangeLogRepositoryPort changeLog;
    private ConfirmEmailChangeUseCase useCase;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepositoryPort();
        tokens = new InMemoryEmailChangeTokenRepositoryPort();
        changeLog = new InMemoryUserProfileChangeLogRepositoryPort();
        useCase = new ConfirmEmailChangeUseCase(tokens, users, changeLog, CLOCK);
        actorId = users.save(new User(
                null, "Admin", "admin@escapa.com", "hash", UserTypes.ADMIN, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK))).getId();
    }

    @Test
    void shouldConfirmAndUpdateTheUserEmail() {
        final User target = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));
        final String raw = "token-ok";
        tokens.save(EmailChangeToken.issue(
                target.getId(),
                "ana.nova@escapa.com",
                EmailChangeToken.hashToken(raw),
                actorId,
                LocalDateTime.now(CLOCK),
                72));

        useCase.execute(raw);

        assertEquals("ana.nova@escapa.com", users.findById(target.getId()).orElseThrow().getEmail());
        assertEquals(1, changeLog.entries().size());
        assertEquals("email", changeLog.entries().get(0).fieldName());
        assertEquals("ana@escapa.com", changeLog.entries().get(0).oldValue());
        assertEquals("ana.nova@escapa.com", changeLog.entries().get(0).newValue());
        assertTrue(tokens.findByTokenHash(EmailChangeToken.hashToken(raw)).orElseThrow().isUsed());
    }

    @Test
    void shouldRejectUnknownToken() {
        assertThrows(EmailChangeTokenNotFoundException.class, () -> useCase.execute("inexistente"));
    }

    @Test
    void shouldRejectWhenUserDisappears() {
        final String raw = "token-orfao";
        tokens.save(EmailChangeToken.issue(
                UUID.randomUUID(),
                "ana.nova@escapa.com",
                EmailChangeToken.hashToken(raw),
                actorId,
                LocalDateTime.now(CLOCK),
                72));

        assertThrows(UserNotFoundException.class, () -> useCase.execute(raw));
    }

    @Test
    void shouldRejectWhenNewEmailIsAlreadyTaken() {
        users.save(new User(
                null, "Outro", "ocupado@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));
        final User target = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));
        final String raw = "token-conflito";
        tokens.save(EmailChangeToken.issue(
                target.getId(),
                "ocupado@escapa.com",
                EmailChangeToken.hashToken(raw),
                actorId,
                LocalDateTime.now(CLOCK),
                72));

        assertThrows(DuplicateRegistrationException.class, () -> useCase.execute(raw));
    }
}
