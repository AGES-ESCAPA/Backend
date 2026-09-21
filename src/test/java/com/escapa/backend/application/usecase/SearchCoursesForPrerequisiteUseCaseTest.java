package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchCoursesForPrerequisiteUseCaseTest {

    @Test
    void shouldExcludeTheCourseItselfFromResults() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course("Fundamentos de Design", admin));
        final Course other = courseRepository.save(course("Fundamentos de Vendas", admin));
        final SearchCoursesForPrerequisiteUseCase useCase =
                new SearchCoursesForPrerequisiteUseCase(courseRepository);

        final List<Course> results = useCase.execute(course.getId(), "Fundamentos");

        assertEquals(1, results.size());
        assertEquals(other.getId(), results.get(0).getId());
        assertFalse(results.stream().anyMatch(c -> c.getId().equals(course.getId())));
    }

    @Test
    void shouldReturnEmptyWhenNoTitleMatches() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final User admin = adminUser();
        final Course course = courseRepository.save(course("Curso A", admin));
        final SearchCoursesForPrerequisiteUseCase useCase =
                new SearchCoursesForPrerequisiteUseCase(courseRepository);

        assertTrue(useCase.execute(course.getId(), "Inexistente").isEmpty());
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
