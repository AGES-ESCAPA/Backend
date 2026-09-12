package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateProgressRulesUseCaseTest {

    @Test
    void shouldUpdateRulesAndLogWhenPublishedCourseChanges() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course(admin));
        course.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(course);
        final UpdateProgressRulesUseCase useCase =
                new UpdateProgressRulesUseCase(courseRepository, changeLogRepository);

        useCase.execute(course.getId(), false, true, admin);

        final Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertFalse(updated.getRequireSequentialProgress());
        assertTrue(updated.getEnforceDeadlineBlock());
        assertEquals(1, updated.getMinorVersion());
        assertEquals(1, changeLogRepository.entriesFor(course.getId()).size());
    }

    @Test
    void shouldNotLogWhenNothingChanges() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course(admin));
        final UpdateProgressRulesUseCase useCase =
                new UpdateProgressRulesUseCase(courseRepository, changeLogRepository);

        useCase.execute(course.getId(), true, false, admin);

        assertTrue(changeLogRepository.entriesFor(course.getId()).isEmpty());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final UpdateProgressRulesUseCase useCase =
                new UpdateProgressRulesUseCase(courseRepository, new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(CourseNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), true, false, null));
    }

    private static User adminUser() {
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        return admin;
    }

    private static Course course(User createdBy) {
        return new Course("Curso", null, null, null, null, null, null, null,
                null, null, null, null, List.of(), true, false, createdBy);
    }
}
