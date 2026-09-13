package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListAdminCoursesUseCaseTest {

    private InMemoryCourseRepositoryPort repository;
    private ListAdminCoursesUseCase useCase;
    private User admin;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCourseRepositoryPort();
        useCase = new ListAdminCoursesUseCase(repository);
        admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
    }

    @Test
    void shouldReturnDraftAndPublishedCoursesSortedByTitle() {
        saveCourse("Gestao de Reservas", CourseStatus.PUBLISHED);
        saveCourse("Atendimento de Excelencia", CourseStatus.PUBLISHED);
        saveCourse("Ingles para Recepcao", CourseStatus.DRAFT);

        final List<Course> result = useCase.execute();

        assertEquals(3, result.size());
        assertEquals("Atendimento de Excelencia", result.get(0).getTitle());
        assertEquals("Gestao de Reservas", result.get(1).getTitle());
        assertEquals("Ingles para Recepcao", result.get(2).getTitle());
    }

    @Test
    void shouldExcludeArchivedCourses() {
        saveCourse("Curso Publicado", CourseStatus.PUBLISHED);
        saveCourse("Curso Arquivado", CourseStatus.ARCHIVED);

        final List<Course> result = useCase.execute();

        assertEquals(1, result.size());
        assertEquals("Curso Publicado", result.get(0).getTitle());
    }

    @Test
    void shouldReturnEmptyListWhenCatalogIsEmpty() {
        assertTrue(useCase.execute().isEmpty());
    }

    private Course saveCourse(String title, CourseStatus status) {
        final Course course = new Course(
                title, null, null, null, null, null, null, null,
                null, null, null, null, List.of(), true, false, admin);
        course.setStatus(status);
        return repository.save(course);
    }
}
