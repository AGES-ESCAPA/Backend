package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.EmailChangeTokenDetails;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.EmailChangeToken;
import com.escapa.backend.domain.user.EmailChangeTokenNotFoundException;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class GetEmailChangeTokenUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK =
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private InMemoryUserRepositoryPort users;
    private InMemoryEmailChangeTokenRepositoryPort tokens;
    private GetEmailChangeTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepositoryPort();
        tokens = new InMemoryEmailChangeTokenRepositoryPort();
        useCase = new GetEmailChangeTokenUseCase(tokens, users, CLOCK);
    }

    @Test
    void shouldReturnDetailsForAnOpenToken() {
        final User user = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));
        final String raw = "token-detalhe";
        tokens.save(EmailChangeToken.issue(
                user.getId(),
                "ana.nova@escapa.com",
                EmailChangeToken.hashToken(raw),
                UUID.randomUUID(),
                LocalDateTime.now(CLOCK),
                72));

        final EmailChangeTokenDetails details = useCase.execute(raw);

        assertEquals("Ana", details.name());
        assertEquals("ana@escapa.com", details.currentEmail());
        assertEquals("ana.nova@escapa.com", details.newEmail());
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
                UUID.randomUUID(),
                LocalDateTime.now(CLOCK),
                72));

        assertThrows(UserNotFoundException.class, () -> useCase.execute(raw));
    }
}
