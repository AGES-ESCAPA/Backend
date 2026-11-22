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
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class RequestAdminEmailChangeUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK =
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private InMemoryUserRepositoryPort users;
    private InMemoryEmailChangeTokenRepositoryPort tokens;
    private RequestAdminEmailChangeUseCase useCase;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepositoryPort();
        tokens = new InMemoryEmailChangeTokenRepositoryPort();
        useCase = new RequestAdminEmailChangeUseCase(
                users, tokens, new FakeContactValidator(), CLOCK, 72);
        actorId = users.save(new User(
                null, "Admin", "admin@escapa.com", "hash", UserTypes.ADMIN, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK))).getId();
    }

    @Test
    void shouldIssueTokenForANewEmail() {
        final User target = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));

        final EmailChangeMailCommand command =
                useCase.execute(actorId, target.getId(), " Ana.Nova@Escapa.com ");

        assertEquals("Ana", command.name());
        assertEquals("ana.nova@escapa.com", command.newEmail());
        assertTrue(tokens.findByTokenHash(EmailChangeToken.hashToken(command.token())).isPresent());
        assertEquals(
                target.getId(),
                tokens.findByTokenHash(EmailChangeToken.hashToken(command.token())).orElseThrow().getUserId());
        assertEquals("ana@escapa.com", users.findById(target.getId()).orElseThrow().getEmail());
    }

    @Test
    void shouldInvalidatePreviousOpenTokens() {
        final User target = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));
        final EmailChangeToken previous = tokens.save(EmailChangeToken.issue(
                target.getId(),
                "antigo@escapa.com",
                EmailChangeToken.hashToken("antigo"),
                actorId,
                LocalDateTime.now(CLOCK),
                72));

        useCase.execute(actorId, target.getId(), "novo@escapa.com");

        assertTrue(tokens.findByTokenHash(previous.getTokenHash()).orElseThrow().isUsed());
    }

    @Test
    void shouldRejectSameEmail() {
        final User target = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(actorId, target.getId(), "ANA@escapa.com"));
    }

    @Test
    void shouldRejectEmailAlreadyTaken() {
        users.save(new User(
                null, "Outro", "ocupado@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));
        final User target = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));

        assertThrows(
                DuplicateRegistrationException.class,
                () -> useCase.execute(actorId, target.getId(), "ocupado@escapa.com"));
    }

    @Test
    void shouldRejectBlankOrInvalidEmail() {
        final User target = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(actorId, target.getId(), null));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(actorId, target.getId(), "  "));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(actorId, target.getId(), "sem-arroba"));
    }

    @Test
    void shouldRejectUnknownUser() {
        assertThrows(
                UserNotFoundException.class,
                () -> useCase.execute(actorId, UUID.randomUUID(), "novo@escapa.com"));
    }
}
