package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCourseUseCaseTest {

    @Test
    void shouldPartiallyUpdateFieldsWithoutTouchingCreatedBy() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo original", null, null, null, null, null, null, null,
                null, null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), "Titulo novo", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, admin);

        assertEquals("Titulo novo", updated.getTitle());
        assertEquals(admin.getId(), updated.getCreatedBy().getId());
    }

    @Test
    void shouldReplaceInstructor() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final User newInstructor = userRepository.save(new User("Instrutora Nova", "nova@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), null, null, null, null, null, newInstructor.getId(), null,
                null, null, null, null, null, null, null, null, admin);

        assertEquals(newInstructor.getId(), updated.getInstructor().getId());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final UpdateCourseUseCase useCase = new UpdateCourseUseCase(
                courseRepository, userRepository, new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(CourseNotFoundException.class, () -> useCase.execute(
                UUID.randomUUID(), "Titulo", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null));
    }

    @Test
    void shouldThrowWhenInstructorIsNotAdmin() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final User student = userRepository.save(new User("Aluno Um", "aluno@escapa.com", "hash", "STUDENT"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                course.getId(), null, null, null, null, null, student.getId(), null,
                null, null, null, null, null, null, null, null, admin));
    }

    @Test
    void shouldBumpMinorVersionAndLogWhenPublishedCourseChanges() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, null, List.of(), true, false, admin));
        course.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(course);
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), "Titulo revisado", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, admin);

        assertEquals(1, updated.getMinorVersion());
        assertEquals(1, changeLogRepository.entriesFor(course.getId()).size());
    }

    @Test
    void shouldNotBumpVersionWhenDraftCourseChanges() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), "Titulo revisado", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, admin);

        assertEquals(0, updated.getMinorVersion());
        assertTrue(changeLogRepository.entriesFor(course.getId()).isEmpty());
    }
}
