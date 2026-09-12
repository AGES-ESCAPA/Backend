package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateCourseUseCaseTest {

    @Test
    void shouldCreateCourseAsDraftWithOnlyTitleRequired() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final CreateCourseUseCase useCase = new CreateCourseUseCase(courseRepository, userRepository);

        final Course course = useCase.execute(
                "Fundamentos de Design", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, admin.getId());

        assertEquals("Fundamentos de Design", course.getTitle());
        assertEquals(CourseStatus.DRAFT, course.getStatus());
        assertEquals(admin.getId(), course.getCreatedBy().getId());
        assertNull(course.getInstructor());
    }

    @Test
    void shouldResolveCreatedByFromTokenAndAcceptInstructorFromPayload() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final User instructor = userRepository.save(new User("Instrutora Um", "instrutora@escapa.com", "hash", "ADMIN"));
        final CreateCourseUseCase useCase = new CreateCourseUseCase(courseRepository, userRepository);

        final Course course = useCase.execute(
                "Fundamentos de Design", "Resumo", "Descricao", null, null, instructor.getId(),
                "Design", "INICIANTE", 40, 365, 365, 499.00,
                List.of("Objetivo 1"), true, false, admin.getId());

        assertEquals(instructor.getId(), course.getInstructor().getId());
        assertEquals(admin.getId(), course.getCreatedBy().getId());
    }

    @Test
    void shouldThrowWhenTitleIsMissing() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final CreateCourseUseCase useCase = new CreateCourseUseCase(courseRepository, userRepository);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                " ", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, admin.getId()));
    }

    @Test
    void shouldThrowWhenInstructorIsNotAdmin() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final User student = userRepository.save(new User("Aluno Um", "aluno@escapa.com", "hash", "STUDENT"));
        final CreateCourseUseCase useCase = new CreateCourseUseCase(courseRepository, userRepository);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                "Curso", null, null, null, null, student.getId(), null, null,
                null, null, null, null, null, null, null, admin.getId()));
    }

    @Test
    void shouldThrowWhenCreatedByUserDoesNotExist() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final CreateCourseUseCase useCase = new CreateCourseUseCase(courseRepository, userRepository);

        assertThrows(UserNotFoundException.class, () -> useCase.execute(
                "Curso", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, UUID.randomUUID()));
    }
}
