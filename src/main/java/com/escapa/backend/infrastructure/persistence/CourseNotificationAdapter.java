package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CourseNotificationPort;
import com.escapa.backend.infrastructure.persistence.entity.NotificationEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.NotificationType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class CourseNotificationAdapter implements CourseNotificationPort {

    private static final String PUBLISHED_TITLE = "Curso publicado";
    private static final String PUBLISHED_MESSAGE = "Uma nova versão do curso foi publicada.";

    private final UserCourseJpaRepository userCourseRepository;
    private final NotificationJpaRepository notificationRepository;
    private final CourseJpaRepository courseJpaRepository;

    public CourseNotificationAdapter(
            UserCourseJpaRepository userCourseRepository,
            NotificationJpaRepository notificationRepository,
            CourseJpaRepository courseJpaRepository) {
        this.userCourseRepository = userCourseRepository;
        this.notificationRepository = notificationRepository;
        this.courseJpaRepository = courseJpaRepository;
    }

    @Override
    public void notifyCoursePublished(UUID courseId) {
        // getReferenceById devolve um proxy: basta a FK, sem recarregar o curso.
        final var course = courseJpaRepository.getReferenceById(courseId);
        userCourseRepository.findActiveByCourseId(courseId, LocalDate.now())
                .forEach(enrollment -> notificationRepository.save(new NotificationEntity(
                        null, enrollment.getUser(), NotificationType.COURSE_PUBLISHED,
                        PUBLISHED_TITLE, PUBLISHED_MESSAGE,
                        course, false, null, LocalDateTime.now())));
    }
}
