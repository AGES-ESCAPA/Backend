package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.LessonNotificationPort;
import com.escapa.backend.infrastructure.persistence.entity.NotificationEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.NotificationType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class LessonNotificationAdapter implements LessonNotificationPort {

    static final String UPDATED_TITLE = "Aula atualizada";

    private final UserContentProgressJpaRepository progressJpaRepository;
    private final NotificationJpaRepository notificationRepository;
    private final ContentJpaRepository contentJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public LessonNotificationAdapter(
            UserContentProgressJpaRepository progressJpaRepository,
            NotificationJpaRepository notificationRepository,
            ContentJpaRepository contentJpaRepository,
            UserJpaRepository userJpaRepository) {
        this.progressJpaRepository = progressJpaRepository;
        this.notificationRepository = notificationRepository;
        this.contentJpaRepository = contentJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    @Transactional
    public void notifyLessonUpdated(UUID lessonId, String lessonTitle) {
        final List<UUID> userIds = progressJpaRepository.findUserIdsByContentId(lessonId);
        if (userIds.isEmpty()) {
            return;
        }
        final var content = contentJpaRepository.getReferenceById(lessonId);
        final var course = content.getModule().getCourse();
        final String message = "A aula \"" + lessonTitle + "\" recebeu atualizações. "
                + "Refaça a aula para conferir o conteúdo novo.";
        final LocalDateTime now = LocalDateTime.now();

        notificationRepository.saveAll(userIds.stream()
                .map(userId -> new NotificationEntity(
                        null, userJpaRepository.getReferenceById(userId), NotificationType.LESSON_UPDATED,
                        UPDATED_TITLE, message, course, content, false, null, now))
                .toList());
    }
}
