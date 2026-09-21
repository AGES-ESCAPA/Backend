package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CourseRules;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static User adminUser() {
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        return admin;
    }

    private static Course course(String title, User createdBy) {
        return new Course(title, null, null, null, null, null, null, null,
                null, null, null, null, List.of(), true, false, createdBy);
    }
}
