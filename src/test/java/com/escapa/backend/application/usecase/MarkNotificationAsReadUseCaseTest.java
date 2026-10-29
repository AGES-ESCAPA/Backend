package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.UserNotification;
import com.escapa.backend.domain.notification.NotificationNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkNotificationAsReadUseCaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);

    private final InMemoryNotificationRepositoryPort repository = new InMemoryNotificationRepositoryPort();
    private final MarkNotificationAsReadUseCase useCase = new MarkNotificationAsReadUseCase(repository, CLOCK);
    private final UUID ownerId = UUID.randomUUID();
    private final UserNotification notification = repository.givenNotification(
            ownerId, UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(CLOCK));

    @Test
    void shouldMarkTheOwnersNotificationAsRead() {
        useCase.execute(ownerId, notification.id());

        assertEquals(0, repository.countUnreadByUserId(ownerId));
        assertTrue(repository.findRecentByUserId(ownerId, 10).get(0).read());
    }

    @Test
    void shouldTreatAnotherUsersNotificationAsNotFound() {
        assertThrows(NotificationNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), notification.id()));
        assertEquals(1, repository.countUnreadByUserId(ownerId));
    }

    @Test
    void shouldRejectUnknownNotification() {
        assertThrows(NotificationNotFoundException.class, () -> useCase.execute(ownerId, UUID.randomUUID()));
    }
}
