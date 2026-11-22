package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.CourseRules;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

class GetCourseRulesUseCaseTest {

    @Test
    void shouldReturnRulesWithPrerequisitesAndRecentChangeLog() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = adminUser();
        changeLogRepository.registerAdmin(admin.getId(), admin.getName());

        final Course course = courseRepository.save(course("Curso A", admin));
        final Course prerequisite = courseRepository.save(course("Curso B", admin));
        prerequisiteRepository.save(course.getId(), prerequisite.getId());
        changeLogRepository.save(course.getId(), admin.getId(), "Versão 1.0 publicada.", 1, 0);

        final GetCourseRulesUseCase useCase =
                new GetCourseRulesUseCase(courseRepository, prerequisiteRepository, changeLogRepository);

        final CourseRules rules = useCase.execute(course.getId());

        assertEquals(true, rules.requireSequentialProgress());
        assertEquals(false, rules.enforceDeadlineBlock());
        assertEquals("0.0", rules.version());
        assertEquals(1, rules.prerequisites().size());
        assertEquals(prerequisite.getId(), rules.prerequisites().get(0).courseId());
        assertEquals(1, rules.recentChangeLog().size());
        assertEquals("Admin Um", rules.recentChangeLog().get(0).changedByName());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final GetCourseRulesUseCase useCase = new GetCourseRulesUseCase(
                courseRepository, new InMemoryCoursePrerequisiteRepositoryPort(),
                new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(CourseNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }

    @Test
    void shouldUseDraftRulesAndOmitMissingDraftPrerequisites() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course("Publicado", admin));
        course.setStatus(CourseStatus.PUBLISHED);
        final Course prerequisite = courseRepository.save(course("Pre-requisito", admin));
        final UUID missingId = UUID.randomUUID();
        course.setPendingDraft(CoursePendingDraft.fromPublished(
                course, List.of(prerequisite.getId(), missingId))
                .withRules(false, true, 80));
        courseRepository.save(course);

        final GetCourseRulesUseCase useCase =
                new GetCourseRulesUseCase(courseRepository, prerequisiteRepository, changeLogRepository);

        final CourseRules rules = useCase.execute(course.getId());

        assertEquals(false, rules.requireSequentialProgress());
        assertEquals(true, rules.enforceDeadlineBlock());
        assertEquals(1, rules.prerequisites().size());
        assertEquals(prerequisite.getId(), rules.prerequisites().get(0).courseId());
    }

    @Test
    void shouldFallBackToPublishedRulesWhenDraftRulesAreNull() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final Course course = courseRepository.save(course("Publicado", adminUser()));
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPendingDraft(new CoursePendingDraft(
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null));
        courseRepository.save(course);
        final GetCourseRulesUseCase useCase = new GetCourseRulesUseCase(
                courseRepository, new InMemoryCoursePrerequisiteRepositoryPort(),
                new InMemoryCourseChangeLogRepositoryPort());

        final CourseRules rules = useCase.execute(course.getId());

        assertEquals(true, rules.requireSequentialProgress());
        assertEquals(false, rules.enforceDeadlineBlock());
        assertTrue(rules.prerequisites().isEmpty());
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
