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

class ListCourseCategoriesUseCaseTest {

    private InMemoryCourseRepositoryPort repository;
    private ListCourseCategoriesUseCase useCase;
    private User admin;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCourseRepositoryPort();
        useCase = new ListCourseCategoriesUseCase(repository);
        admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
    }

    @Test
    void shouldReturnDistinctCategoriesFromEveryCourseStatus() {
        saveCourse("Curso Publicado", "Hospitalidade", CourseStatus.PUBLISHED);
        saveCourse("Curso Rascunho", "Inteligência Artificial", CourseStatus.DRAFT);
        saveCourse("Curso Arquivado", "Marketing", CourseStatus.ARCHIVED);
        saveCourse("Outro Publicado", "Hospitalidade", CourseStatus.PUBLISHED);

        final List<String> categories = useCase.execute();

        assertEquals(List.of("Hospitalidade", "Inteligência Artificial", "Marketing"), categories);
    }

    @Test
    void shouldIgnoreBlankCategoriesAndCollapseCaseVariants() {
        saveCourse("Com espacos", "  Turismo  ", CourseStatus.DRAFT);
        saveCourse("Maiusculas", "MARKETING", CourseStatus.PUBLISHED);
        saveCourse("Minusculas", "marketing", CourseStatus.DRAFT);
        saveCourse("Vazio", "", CourseStatus.DRAFT);
        saveCourse("Em branco", "   ", CourseStatus.DRAFT);
        saveCourse("Sem categoria", null, CourseStatus.DRAFT);

        final List<String> categories = useCase.execute();

        assertEquals(2, categories.size());
        assertTrue(categories.get(0).equalsIgnoreCase("marketing"));
        assertEquals("Turismo", categories.get(1));
    }

    @Test
    void shouldReturnEmptyListWhenNoCategoryIsRegistered() {
        saveCourse("Sem categoria", null, CourseStatus.DRAFT);

        assertTrue(useCase.execute().isEmpty());
    }

    private Course saveCourse(String title, String category, CourseStatus status) {
        final Course course = new Course(
                title, null, null, null, null, null, category, null,
                null, null, null, null, List.of(), true, false, admin);
        course.setStatus(status);
        return repository.save(course);
    }
}
