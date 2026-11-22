package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

class UnarchiveCourseUseCaseTest {

    private InMemoryCourseRepositoryPort repository;
    private UnarchiveCourseUseCase useCase;
    private User admin;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCourseRepositoryPort();
        useCase = new UnarchiveCourseUseCase(repository);
        admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
    }

    @Test
    void shouldRestoreArchivedCourseAsPublished() {
        final Course archived = saveCourse("Curso Arquivado", CourseStatus.ARCHIVED);

        final Course result = useCase.execute(archived.getId());

        assertEquals(CourseStatus.PUBLISHED, result.getStatus());
        assertEquals(CourseStatus.PUBLISHED, repository.findById(archived.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldRejectUnarchiveWhenCourseIsNotArchived() {
        final Course published = saveCourse("Curso Publicado", CourseStatus.PUBLISHED);

        final IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(published.getId()));

        assertEquals("Only archived courses can be unarchived", error.getMessage());
        assertEquals(CourseStatus.PUBLISHED, repository.findById(published.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldRejectUnarchiveWhenCourseDoesNotExist() {
        assertThrows(CourseNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }

    private Course saveCourse(String title, CourseStatus status) {
        final Course course = new Course(
                title, null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin);
        course.setStatus(status);
        return repository.save(course);
    }
}
