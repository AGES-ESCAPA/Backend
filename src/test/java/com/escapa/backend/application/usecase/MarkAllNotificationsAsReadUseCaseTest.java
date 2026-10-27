package com.escapa.backend.application.usecase;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkAllNotificationsAsReadUseCaseTest {

    private final InMemoryNotificationRepositoryPort repository = new InMemoryNotificationRepositoryPort();
    private final MarkAllNotificationsAsReadUseCase useCase =
            new MarkAllNotificationsAsReadUseCase(repository, Clock.systemUTC());

    @Test
    void shouldMarkOnlyTheNotificationsOfTheGivenUser() {
        final UUID userId = UUID.randomUUID();
        final UUID otherUserId = UUID.randomUUID();
        repository.givenNotification(userId, null, null, LocalDateTime.now());
        repository.givenNotification(userId, null, null, LocalDateTime.now());
        repository.givenNotification(otherUserId, null, null, LocalDateTime.now());

        useCase.execute(userId);

        assertEquals(0, repository.countUnreadByUserId(userId));
        assertEquals(1, repository.countUnreadByUserId(otherUserId));
    }
}
