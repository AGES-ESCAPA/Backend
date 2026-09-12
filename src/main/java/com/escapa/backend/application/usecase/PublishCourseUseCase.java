package com.escapa.backend.application.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.adapters.dto.PublishCourseRequest;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.NotificationJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserCourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.NotificationEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;
import com.escapa.backend.infrastructure.persistence.entity.enums.NotificationType;

public class PublishCourseUseCase {
    private final CourseRepositoryPort courseRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;
    private final UserCourseJpaRepository userCourseRepository;
    private final NotificationJpaRepository notificationRepository;

    public PublishCourseUseCase(
            CourseRepositoryPort courseRepository,
            CourseChangeLogRepositoryPort changeLogRepository,
            UserCourseJpaRepository userCourseRepository,
            NotificationJpaRepository notificationRepository) {
        this.courseRepository = courseRepository;
        this.changeLogRepository = changeLogRepository;
        this.userCourseRepository = userCourseRepository;
        this.notificationRepository = notificationRepository;
    }

    public CourseEntity execute(UUID courseId, PublishCourseRequest request, UserEntity changedBy) {
        final CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Curso não encontrado."));
        course.setMajorVersion(course.getMajorVersion() + 1);
        course.setMinorVersion(0);
        course.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(course);
        changeLogRepository.save(new CourseChangeLogEntity(
                null, course, changedBy,
                "Versão " + course.getMajorVersion() + ".0 publicada.",
                course.getMajorVersion(), course.getMinorVersion(), LocalDateTime.now()));
        if (Boolean.TRUE.equals(request.notifyEnrolledStudents())) {
            userCourseRepository.findActiveByCourseId(courseId, LocalDate.now())
                    .forEach(enrollment -> notificationRepository.save(new NotificationEntity(
                            null, enrollment.getUser(), NotificationType.COURSE_PUBLISHED,
                            "Curso publicado", "Uma nova versão do curso foi publicada.",
                            course, false, null, LocalDateTime.now())));
        }
        return course;
    }
}
