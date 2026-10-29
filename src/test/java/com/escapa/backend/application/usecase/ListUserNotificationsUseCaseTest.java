package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.UserNotification;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListUserNotificationsUseCaseTest {

    private final InMemoryNotificationRepositoryPort repository = new InMemoryNotificationRepositoryPort();
    private final ListUserNotificationsUseCase useCase = new ListUserNotificationsUseCase(repository);
    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldListTheNewestNotificationsFirstWithTheUnreadCount() {
        final LocalDateTime now = LocalDateTime.of(2026, 10, 27, 10, 0);
        final UserNotification older = repository.givenNotification(userId, null, null, now.minusDays(1));
        final UserNotification newer = repository.givenNotification(userId, null, null, now);
        repository.givenNotification(UUID.randomUUID(), null, null, now);
        repository.markAsRead(userId, older.id(), now);

        final ListUserNotificationsUseCase.Result result = useCase.execute(userId);

        assertEquals(List.of(newer.id(), older.id()),
                result.notifications().stream().map(UserNotification::id).toList());
        assertEquals(1, result.unreadCount());
    }

    @Test
    void shouldLimitTheNumberOfNotifications() {
        final LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < ListUserNotificationsUseCase.MAX_NOTIFICATIONS + 5; index++) {
            repository.givenNotification(userId, null, null, now.minusMinutes(index));
        }

        final ListUserNotificationsUseCase.Result result = useCase.execute(userId);

        assertEquals(ListUserNotificationsUseCase.MAX_NOTIFICATIONS, result.notifications().size());
        assertEquals(ListUserNotificationsUseCase.MAX_NOTIFICATIONS + 5, result.unreadCount());
    }
}
