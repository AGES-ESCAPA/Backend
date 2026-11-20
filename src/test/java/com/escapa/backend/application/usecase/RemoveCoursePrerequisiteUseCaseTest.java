package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.course.CourseStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoveCoursePrerequisiteUseCaseTest {

    @Test
    void shouldRemovePrerequisiteAndLogChange() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course("Curso A", admin));
        final Course prerequisite = courseRepository.save(course("Curso B", admin));
        prerequisiteRepository.save(course.getId(), prerequisite.getId());
        final RemoveCoursePrerequisiteUseCase useCase =
                new RemoveCoursePrerequisiteUseCase(courseRepository, prerequisiteRepository, changeLogRepository);

        useCase.execute(course.getId(), prerequisite.getId(), admin);

        assertFalse(prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                course.getId(), prerequisite.getId()));
        assertEquals(1, changeLogRepository.entriesFor(course.getId()).size());
    }

    @Test
    void shouldThrowWhenPrerequisiteNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course("Curso A", admin));
        final RemoveCoursePrerequisiteUseCase useCase = new RemoveCoursePrerequisiteUseCase(
                courseRepository, new InMemoryCoursePrerequisiteRepositoryPort(),
                new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(course.getId(), UUID.randomUUID(), admin));
    }

    @Test
    void shouldKeepPublishedPrerequisiteRemovalInTheDraft() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisites =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLog = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course("Curso A", admin));
        final Course prerequisite = courseRepository.save(course("Curso B", admin));
        prerequisites.save(course.getId(), prerequisite.getId());
        course.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(course);
        final RemoveCoursePrerequisiteUseCase useCase =
                new RemoveCoursePrerequisiteUseCase(courseRepository, prerequisites, changeLog);

        useCase.execute(course.getId(), prerequisite.getId(), admin);

        assertTrue(prerequisites.existsByCourseIdAndPrerequisiteCourseId(
                course.getId(), prerequisite.getId()));
        assertFalse(courseRepository.findById(course.getId()).orElseThrow()
                .getPendingDraft().containsPrerequisite(prerequisite.getId()));
        assertTrue(changeLog.entriesFor(course.getId()).isEmpty());
    }

    @Test
    void shouldRejectMissingPrerequisiteFromPublishedDraft() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final Course course = courseRepository.save(course("Curso A", adminUser()));
        course.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(course);
        final RemoveCoursePrerequisiteUseCase useCase = new RemoveCoursePrerequisiteUseCase(
                courseRepository, new InMemoryCoursePrerequisiteRepositoryPort(),
                new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(course.getId(), UUID.randomUUID(), null));
    }

    private static User adminUser() {
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        return admin;
    }

    private static Course course(String title, User createdBy) {
        return new Course(title, null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, createdBy);
    }
}
