package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.application.model.UserNotification;
import com.escapa.backend.application.usecase.InMemoryNotificationRepositoryPort;
import com.escapa.backend.application.usecase.ListUserNotificationsUseCase;
import com.escapa.backend.application.usecase.MarkAllNotificationsAsReadUseCase;
import com.escapa.backend.application.usecase.MarkNotificationAsReadUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {

    private static final String NOTIFICATIONS_URL = "/api/v1/notifications";

    private final InMemoryNotificationRepositoryPort repository = new InMemoryNotificationRepositoryPort();
    private final UUID studentId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID lessonId = UUID.randomUUID();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final NotificationController controller = new NotificationController(
                new ListUserNotificationsUseCase(repository),
                new MarkNotificationAsReadUseCase(repository, Clock.systemUTC()),
                new MarkAllNotificationsAsReadUseCase(repository, Clock.systemUTC())
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authenticate(studentId);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(UUID userId) {
        final var user = new AuthenticatedUser(userId, "aluno@escapa.com", "STUDENT", "STUDENT");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, AuthorityUtils.createAuthorityList("ROLE_STUDENT")));
    }

    @Test
    void shouldRejectRequestsWithoutAuthentication() throws Exception {
        SecurityContextHolder.clearContext();
        mockMvc.perform(get(NOTIFICATIONS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing or invalid access token"));
    }

    @Test
    void shouldListOnlyTheAuthenticatedUsersNotifications() throws Exception {
        repository.givenNotification(studentId, courseId, lessonId, LocalDateTime.now());
        repository.givenNotification(UUID.randomUUID(), courseId, lessonId, LocalDateTime.now());

        mockMvc.perform(get(NOTIFICATIONS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(1))
                .andExpect(jsonPath("$.data.notifications.length()").value(1))
                .andExpect(jsonPath("$.data.notifications[0].type").value("LESSON_UPDATED"))
                .andExpect(jsonPath("$.data.notifications[0].courseId").value(courseId.toString()))
                .andExpect(jsonPath("$.data.notifications[0].lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.data.notifications[0].read").value(false));
    }

    @Test
    void shouldMarkANotificationAsRead() throws Exception {
        final UserNotification notification =
                repository.givenNotification(studentId, courseId, lessonId, LocalDateTime.now());

        mockMvc.perform(patch(NOTIFICATIONS_URL + "/{id}/read", notification.id()))
                .andExpect(status().isNoContent());

        assertEquals(0, repository.countUnreadByUserId(studentId));
    }

    @Test
    void shouldAnswerNotFoundForAnotherUsersNotification() throws Exception {
        final UserNotification notification =
                repository.givenNotification(UUID.randomUUID(), courseId, lessonId, LocalDateTime.now());

        mockMvc.perform(patch(NOTIFICATIONS_URL + "/{id}/read", notification.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldMarkEveryNotificationAsRead() throws Exception {
        repository.givenNotification(studentId, courseId, lessonId, LocalDateTime.now());
        repository.givenNotification(studentId, courseId, lessonId, LocalDateTime.now());

        mockMvc.perform(patch(NOTIFICATIONS_URL + "/read-all"))
                .andExpect(status().isNoContent());

        assertEquals(0, repository.countUnreadByUserId(studentId));
    }

    @Test
    void shouldRejectAnInvalidNotificationId() throws Exception {
        mockMvc.perform(patch(NOTIFICATIONS_URL + "/{id}/read", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
