package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

class AddCoursePrerequisiteUseCaseTest {

    @Test
    void shouldAddPrerequisiteAndLogChange() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course("Curso A", admin));
        final Course prerequisite = courseRepository.save(course("Curso B", admin));
        final AddCoursePrerequisiteUseCase useCase =
                new AddCoursePrerequisiteUseCase(courseRepository, prerequisiteRepository, changeLogRepository);

        useCase.execute(course.getId(), prerequisite.getId(), admin);

        assertTrue(prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                course.getId(), prerequisite.getId()));
        assertEquals(1, changeLogRepository.entriesFor(course.getId()).size());
        assertEquals("Pré-requisito adicionado.",
                changeLogRepository.entriesFor(course.getId()).get(0).description());
    }

    @Test
    void shouldKeepPublishedPrerequisitesUntilTheCourseIsPublishedAgain() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course("Curso A", admin));
        course.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(course);
        final Course prerequisite = courseRepository.save(course("Curso B", admin));
        final AddCoursePrerequisiteUseCase useCase =
                new AddCoursePrerequisiteUseCase(courseRepository, prerequisiteRepository,
                        new InMemoryCourseChangeLogRepositoryPort());

        useCase.execute(course.getId(), prerequisite.getId(), admin);

        assertFalse(prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                course.getId(), prerequisite.getId()));
        assertTrue(courseRepository.findById(course.getId()).orElseThrow()
                .getPendingDraft().prerequisiteIds().contains(prerequisite.getId()));
    }

    @Test
    void shouldRejectSelfReference() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course("Curso A", admin));
        final AddCoursePrerequisiteUseCase useCase = new AddCoursePrerequisiteUseCase(
                courseRepository, prerequisiteRepository, new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(course.getId(), course.getId(), admin));
    }

    @Test
    void shouldRejectDuplicate() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course("Curso A", admin));
        final Course prerequisite = courseRepository.save(course("Curso B", admin));
        prerequisiteRepository.save(course.getId(), prerequisite.getId());
        final AddCoursePrerequisiteUseCase useCase = new AddCoursePrerequisiteUseCase(
                courseRepository, prerequisiteRepository, new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(course.getId(), prerequisite.getId(), admin));
    }

    @Test
    void shouldRejectCycle() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final User admin = adminUser();
        final Course courseA = courseRepository.save(course("Curso A", admin));
        final Course courseB = courseRepository.save(course("Curso B", admin));
        // B ja exige A; adicionar A exigindo B formaria um ciclo.
        prerequisiteRepository.save(courseB.getId(), courseA.getId());
        final AddCoursePrerequisiteUseCase useCase = new AddCoursePrerequisiteUseCase(
                courseRepository, prerequisiteRepository, new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(courseA.getId(), courseB.getId(), admin));
    }

    @Test
    void shouldRejectCycleCreatedOnlyThroughDrafts() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final User admin = adminUser();
        final Course courseA = courseRepository.save(course("Curso A", admin));
        final Course courseB = courseRepository.save(course("Curso B", admin));
        courseA.setStatus(CourseStatus.PUBLISHED);
        courseB.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(courseA);
        courseRepository.save(courseB);
        final AddCoursePrerequisiteUseCase useCase = new AddCoursePrerequisiteUseCase(
                courseRepository, prerequisiteRepository, new InMemoryCourseChangeLogRepositoryPort());

        useCase.execute(courseA.getId(), courseB.getId(), admin);

        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(courseB.getId(), courseA.getId(), admin));
    }

    @Test
    void shouldIgnoreRemovedDraftPrerequisiteWhenCheckingCycle() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final User admin = adminUser();
        final Course courseA = courseRepository.save(course("Curso A", admin));
        final Course courseB = courseRepository.save(course("Curso B", admin));
        courseA.setStatus(CourseStatus.PUBLISHED);
        courseB.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(courseA);
        courseRepository.save(courseB);
        prerequisiteRepository.save(courseA.getId(), courseB.getId());
        // Publicado A exige B, mas o rascunho de A já removeu B.
        courseA.setPendingDraft(CoursePendingDraft.fromPublished(courseA, List.of()));
        courseRepository.save(courseA);
        final AddCoursePrerequisiteUseCase useCase = new AddCoursePrerequisiteUseCase(
                courseRepository, prerequisiteRepository, new InMemoryCourseChangeLogRepositoryPort());

        useCase.execute(courseB.getId(), courseA.getId(), admin);

        assertTrue(courseRepository.findById(courseB.getId()).orElseThrow()
                .getPendingDraft().prerequisiteIds().contains(courseA.getId()));
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final AddCoursePrerequisiteUseCase useCase = new AddCoursePrerequisiteUseCase(
                courseRepository, new InMemoryCoursePrerequisiteRepositoryPort(),
                new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(CourseNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), UUID.randomUUID(), null));
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
