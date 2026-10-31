package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateCourseUseCaseTest {

    @Test
    void shouldPartiallyUpdateFieldsWithoutTouchingCreatedBy() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo original", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, new InMemoryCoursePrerequisiteRepositoryPort());

        final Course updated = useCase.execute(
                course.getId(), "Titulo novo", null, null, null, null, null, null,
                null, null, null, null, null, null, null, admin);

        assertEquals("Titulo novo", updated.getTitle());
        assertEquals(admin.getId(), updated.getCreatedBy().getId());
    }

    @Test
    void shouldReplaceInstructor() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final User newInstructor = userRepository.save(new User("Instrutora Nova", "nova@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, new InMemoryCoursePrerequisiteRepositoryPort());

        final Course updated = useCase.execute(
                course.getId(), null, null, null, null, null, newInstructor.getId(), null,
                null, null, null, null, null, null, null, admin);

        assertEquals(newInstructor.getId(), updated.getInstructor().getId());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final UpdateCourseUseCase useCase = new UpdateCourseUseCase(
                courseRepository, userRepository, new InMemoryCoursePrerequisiteRepositoryPort());

        assertThrows(CourseNotFoundException.class, () -> useCase.execute(
                UUID.randomUUID(), "Titulo", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null));
    }

    @Test
    void shouldThrowWhenInstructorIsNotAdmin() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final User student = userRepository.save(new User("Aluno Um", "aluno@escapa.com", "hash", "STUDENT"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, new InMemoryCoursePrerequisiteRepositoryPort());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                course.getId(), null, null, null, null, null, student.getId(), null,
                null, null, null, null, null, null, null, admin));
    }

    @Test
    void shouldKeepPublishedCourseUntouchedWhenSavingADraft() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPrice(100.0);
        courseRepository.save(course);
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, new InMemoryCoursePrerequisiteRepositoryPort());

        final Course updated = useCase.execute(
                course.getId(), "Titulo revisado", null, null, null, null, null, null,
                null, null, null, 250.0, null, null, null, admin);

        assertEquals("Titulo revisado", updated.getTitle());
        assertEquals(250.0, updated.getPrice());
        assertEquals(CourseStatus.PUBLISHED, updated.getStatus());

        final Course published = courseRepository.findById(course.getId()).orElseThrow();
        assertEquals("Titulo", published.getTitle());
        assertEquals(100.0, published.getPrice());
        assertEquals(0, published.getMinorVersion());
        assertEquals("Titulo revisado", published.getPendingDraft().title());
        assertEquals(250.0, published.getPendingDraft().price());
    }

    @Test
    void shouldNotBumpVersionWhenDraftCourseChanges() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, new InMemoryCoursePrerequisiteRepositoryPort());

        final Course updated = useCase.execute(
                course.getId(), "Titulo revisado", null, null, null, null, null, null,
                null, null, null, null, null, null, null, admin);

        assertEquals("Titulo revisado", updated.getTitle());
        assertEquals(0, updated.getMinorVersion());
        assertNull(updated.getPendingDraft());
    }
}
