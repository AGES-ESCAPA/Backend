package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PublishedCourseFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListPublishedCourseFiltersUseCaseTest {

    private InMemoryCourseRepositoryPort repository;
    private ListPublishedCourseFiltersUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCourseRepositoryPort();
        useCase = new ListPublishedCourseFiltersUseCase(repository);
        repository.addCourse(buildCourse("IA Aplicada", "Inteligência Artificial", "AVANCADO"));
        repository.addCourse(buildCourse("Atendimento", "Hospitalidade", "INICIANTE"));
        repository.addCourse(buildCourse("Reservas", "Hospitalidade", "INTERMEDIARIO"));
    }

    @Test
    void shouldReturnDistinctCategoriesAndLevelsInDisplayOrder() {
        final PublishedCourseFilters filters = useCase.execute();

        assertEquals(java.util.List.of("Hospitalidade", "Inteligência Artificial"), filters.categories());
        assertEquals(java.util.List.of("INICIANTE", "INTERMEDIARIO", "AVANCADO"), filters.levels());
    }

    private static CourseSummary buildCourse(String title, String category, String level) {
        return new CourseSummary(
                UUID.randomUUID(), title, "resumo", category, level,
                60, 1, 10.0, null, "Instrutor", 4.0, 1);
    }
}
