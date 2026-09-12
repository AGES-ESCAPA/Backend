package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.course.CourseValidationException;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishCourseUseCaseTest {

    @Test
    void shouldPublishWhenAllRequiredFieldsAndContentArePresent() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final InMemoryCourseNotificationPort notificationPort = new InMemoryCourseNotificationPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(completeCourse(admin));
        final PublishCourseUseCase useCase =
                new PublishCourseUseCase(courseRepository, changeLogRepository, notificationPort);

        final Course published = useCase.execute(course.getId(), false, admin);

        assertEquals(CourseStatus.PUBLISHED, published.getStatus());
        assertEquals(1, published.getMajorVersion());
        assertEquals(0, published.getMinorVersion());
        assertEquals(1, changeLogRepository.entriesFor(course.getId()).size());
        assertTrue(notificationPort.notifiedCourseIds().isEmpty());
    }

    @Test
    void shouldNotifyEnrolledStudentsWhenRequested() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final InMemoryCourseNotificationPort notificationPort = new InMemoryCourseNotificationPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(completeCourse(admin));
        final PublishCourseUseCase useCase =
                new PublishCourseUseCase(courseRepository, changeLogRepository, notificationPort);

        useCase.execute(course.getId(), true, admin);

        assertEquals(List.of(course.getId()), notificationPort.notifiedCourseIds());
    }

    @Test
    void shouldThrowWithMissingFieldsWhenIncomplete() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final InMemoryCourseNotificationPort notificationPort = new InMemoryCourseNotificationPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, null, List.of(), true, false, admin));
        final PublishCourseUseCase useCase =
                new PublishCourseUseCase(courseRepository, changeLogRepository, notificationPort);

        final CourseValidationException ex = assertThrows(
                CourseValidationException.class, () -> useCase.execute(course.getId(), false, admin));

        assertTrue(ex.getMissingFields().contains("description"));
        assertTrue(ex.getMissingFields().contains("instructorId"));
        assertTrue(ex.getMissingFields().stream().anyMatch(field -> field.startsWith("modules")));
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final PublishCourseUseCase useCase = new PublishCourseUseCase(
                courseRepository, new InMemoryCourseChangeLogRepositoryPort(), new InMemoryCourseNotificationPort());

        assertThrows(CourseNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), false, null));
    }

    private static Course completeCourse(User instructor) {
        final Course course = new Course(
                "Fundamentos de Design", "Resumo curto", "Descricao completa", null, null,
                instructor, "Design", "INICIANTE", 40, 365, 365, 499.00,
                List.of("Objetivo 1"), true, false, instructor);

        final Module module = new Module(UUID.randomUUID(), course, "Modulo 1", 1);
        final Content content = new Content();
        content.setId(UUID.randomUUID());
        content.setTitle("Aula 1");
        module.getContents().add(content);
        course.getModules().add(module);

        return course;
    }
}
