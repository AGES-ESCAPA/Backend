package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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

class UpdateProgressRulesUseCaseTest {

    @Test
    void shouldKeepPublishedRulesUntilTheCourseIsPublishedAgain() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course(admin));
        course.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(course);
        final UpdateProgressRulesUseCase useCase =
                new UpdateProgressRulesUseCase(courseRepository, changeLogRepository,
                new InMemoryCoursePrerequisiteRepositoryPort());

        useCase.execute(course.getId(), false, true, 70, admin);

        final Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertTrue(updated.getRequireSequentialProgress());
        assertFalse(updated.getEnforceDeadlineBlock());
        assertEquals(0, updated.getMinorVersion());
        assertEquals(false, updated.getPendingDraft().requireSequentialProgress());
        assertEquals(true, updated.getPendingDraft().enforceDeadlineBlock());
        assertTrue(changeLogRepository.entriesFor(course.getId()).isEmpty());
    }

    @Test
    void shouldNotLogWhenNothingChanges() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course(admin));
        final UpdateProgressRulesUseCase useCase =
                new UpdateProgressRulesUseCase(courseRepository, changeLogRepository,
                new InMemoryCoursePrerequisiteRepositoryPort());

        useCase.execute(course.getId(), true, false, 70, admin);

        assertTrue(changeLogRepository.entriesFor(course.getId()).isEmpty());
    }

    @Test
    void shouldLogChangedRulesWhenNoAdminIsProvided() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final Course course = courseRepository.save(course(adminUser()));
        final UpdateProgressRulesUseCase useCase =
                new UpdateProgressRulesUseCase(courseRepository, changeLogRepository,
                        new InMemoryCoursePrerequisiteRepositoryPort());

        useCase.execute(course.getId(), false, true, 70, null);

        final Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertFalse(updated.getRequireSequentialProgress());
        assertTrue(updated.getEnforceDeadlineBlock());
        assertEquals(1, changeLogRepository.entriesFor(course.getId()).size());
        assertEquals("Alteração nas regras de progressão.",
                changeLogRepository.entriesFor(course.getId()).get(0).description());
    }

    @Test
    void shouldNotSavePublishedDraftWhenRequestedRulesAlreadyMatch() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final Course course = course(adminUser());
        course.setStatus(CourseStatus.PUBLISHED);
        final CoursePendingDraft draftOriginal = new CoursePendingDraft(
                null, null, null, null, null, null, null, null, null, null, null,
                null, false, true, 70, List.of());
        course.setPendingDraft(draftOriginal);
        courseRepository.save(course);
        final int savesBeforeAct = courseRepository.saveCount();
        final UpdateProgressRulesUseCase useCase =
                new UpdateProgressRulesUseCase(courseRepository, changeLogRepository,
                        new InMemoryCoursePrerequisiteRepositoryPort());

        useCase.execute(course.getId(), false, true, 70, null);

        final Course stored = courseRepository.findById(course.getId()).orElseThrow();
        assertEquals(savesBeforeAct, courseRepository.saveCount());
        assertSame(draftOriginal, stored.getPendingDraft());
        assertTrue(changeLogRepository.entriesFor(course.getId()).isEmpty());
    }

    @Test
    void shouldPersistMinQuizPassPercentageOnDraftCourse() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final Course course = courseRepository.save(course(adminUser()));
        final UpdateProgressRulesUseCase useCase =
                new UpdateProgressRulesUseCase(courseRepository, new InMemoryCourseChangeLogRepositoryPort(),
                        new InMemoryCoursePrerequisiteRepositoryPort());

        useCase.execute(course.getId(), true, false, 85, null);

        assertEquals(85, courseRepository.findById(course.getId()).orElseThrow().getMinQuizPassPercentage());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final UpdateProgressRulesUseCase useCase =
                new UpdateProgressRulesUseCase(courseRepository, new InMemoryCourseChangeLogRepositoryPort(),
                        new InMemoryCoursePrerequisiteRepositoryPort());

        assertThrows(CourseNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), true, false, 70, null));
    }

    private static User adminUser() {
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        return admin;
    }

    private static Course course(User createdBy) {
        return new Course("Curso", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, createdBy);
    }
}
