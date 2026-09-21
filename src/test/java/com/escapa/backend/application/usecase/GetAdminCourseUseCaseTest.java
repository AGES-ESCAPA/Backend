package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetAdminCourseUseCaseTest {

    @Test
    void shouldReturnCourseRegardlessOfStatus() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, null, List.of(), true, false, admin));
        final GetAdminCourseUseCase useCase = new GetAdminCourseUseCase(courseRepository);

        final Course found = useCase.execute(course.getId());

        assertEquals(course.getId(), found.getId());
        assertEquals("Titulo", found.getTitle());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final GetAdminCourseUseCase useCase = new GetAdminCourseUseCase(courseRepository);

        assertThrows(CourseNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }
}
