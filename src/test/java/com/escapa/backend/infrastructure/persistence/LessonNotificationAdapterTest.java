package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.UserNotification;
import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.application.port.LessonNotificationPort;
import com.escapa.backend.application.port.NotificationRepositoryPort;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserContentProgressEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserContentProgressId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonNotificationAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private LessonNotificationPort lessonNotificationPort;

    @Autowired
    private NotificationRepositoryPort notificationRepositoryPort;

    @Autowired
    private ContentRepositoryPort contentRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private ModuleJpaRepository moduleJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserContentProgressJpaRepository progressJpaRepository;

    private UUID givenUser() {
        final UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setName("Aluno de teste");
        user.setEmail("aluno-" + UUID.randomUUID() + "@teste.com");
        user.setPasswordHash("hash");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        return userJpaRepository.save(user).getId();
    }

    private UUID givenModule() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Curso de teste");
        final ModuleEntity module = new ModuleEntity();
        module.setCourse(courseJpaRepository.save(course));
        module.setTitle("Modulo de teste");
        module.setOrder(1);
        return moduleJpaRepository.save(module).getId();
    }

    private UUID givenContent(UUID moduleId) {
        final Content content = new Content();
        content.setModuleId(moduleId);
        content.setTitle("Aula de teste");
        content.setType(ContentType.TEXT);
        content.setDescription("Texto");
        content.setIsFree(false);
        content.setOrder(contentRepositoryPort.nextOrder(moduleId));
        return contentRepositoryPort.save(content).getId();
    }

    private void givenProgress(UUID userId, UUID contentId) {
        final UserContentProgressEntity progress = new UserContentProgressEntity();
        progress.setId(new UserContentProgressId(userId, contentId));
        progress.setCompletedAt(LocalDateTime.now());
        progressJpaRepository.save(progress);
    }

    @Test
    void shouldNotifyOnlyStudentsWhoCompletedTheLesson() {
        final UUID moduleId = givenModule();
        final UUID courseId = moduleJpaRepository.findById(moduleId).orElseThrow().getCourse().getId();
        final UUID lessonId = givenContent(moduleId);
        final UUID completedUser = givenUser();
        final UUID otherUser = givenUser();
        givenProgress(completedUser, lessonId);

        lessonNotificationPort.notifyLessonUpdated(lessonId, "Aula revisada");

        final List<UserNotification> notifications = notificationRepositoryPort.findRecentByUserId(completedUser, 10);
        assertEquals(1, notifications.size());
        final UserNotification notification = notifications.get(0);
        assertEquals("LESSON_UPDATED", notification.type());
        assertEquals(courseId, notification.courseId());
        assertEquals(lessonId, notification.lessonId());
        assertTrue(notification.message().contains("Aula revisada"));
        assertFalse(notification.read());
        assertEquals(1, notificationRepositoryPort.countUnreadByUserId(completedUser));
        assertTrue(notificationRepositoryPort.findRecentByUserId(otherUser, 10).isEmpty());
    }

    @Test
    void shouldMarkNotificationsAsReadOnlyForTheirOwner() {
        final UUID moduleId = givenModule();
        final UUID lessonId = givenContent(moduleId);
        final UUID owner = givenUser();
        final UUID stranger = givenUser();
        givenProgress(owner, lessonId);
        lessonNotificationPort.notifyLessonUpdated(lessonId, "Aula revisada");
        final UUID notificationId = notificationRepositoryPort.findRecentByUserId(owner, 10).get(0).id();

        assertFalse(notificationRepositoryPort.markAsRead(stranger, notificationId, LocalDateTime.now()));
        assertEquals(1, notificationRepositoryPort.countUnreadByUserId(owner));

        assertTrue(notificationRepositoryPort.markAsRead(owner, notificationId, LocalDateTime.now()));
        assertTrue(notificationRepositoryPort.markAsRead(owner, notificationId, LocalDateTime.now()));
        assertEquals(0, notificationRepositoryPort.countUnreadByUserId(owner));
    }
}
