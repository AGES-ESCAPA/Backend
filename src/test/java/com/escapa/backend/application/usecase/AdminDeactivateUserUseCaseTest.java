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

import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.CannotDeactivateOwnAccountException;
import com.escapa.backend.domain.user.UserAlreadyInactiveException;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

class AdminDeactivateUserUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final Clock CLOCK =
            Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private InMemoryUserRepositoryPort users;
    private InMemoryAccountInviteRepositoryPort invites;
    private AdminDeactivateUserUseCase useCase;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepositoryPort();
        invites = new InMemoryAccountInviteRepositoryPort();
        useCase = new AdminDeactivateUserUseCase(users, invites, CLOCK);
        actorId = users.save(new User(
                null, "Admin", "admin@escapa.com", "hash", UserTypes.ADMIN, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK))).getId();
    }

    @Test
    void shouldDeactivateAnActiveUser() {
        final User target = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.ACTIVE, LocalDateTime.now(CLOCK)));

        final User deactivated = useCase.execute(actorId, target.getId());

        assertEquals(UserStatus.INACTIVE, deactivated.getStatus());
        assertEquals(UserStatus.INACTIVE, users.findById(target.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldRejectDeactivatingOwnAccount() {
        assertThrows(CannotDeactivateOwnAccountException.class, () -> useCase.execute(actorId, actorId));
    }

    @Test
    void shouldRejectAlreadyInactiveUser() {
        final User target = users.save(new User(
                null, "Ana", "ana@escapa.com", "hash", UserTypes.STUDENT, null,
                UserStatus.INACTIVE, LocalDateTime.now(CLOCK)));

        assertThrows(UserAlreadyInactiveException.class, () -> useCase.execute(actorId, target.getId()));
    }

    @Test
    void shouldRejectUnknownUser() {
        assertThrows(UserNotFoundException.class, () -> useCase.execute(actorId, UUID.randomUUID()));
    }
}
