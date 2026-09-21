package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListPublishedCoursesUseCaseTest {

    private InMemoryCourseRepositoryPort repository;
    private ListPublishedCoursesUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCourseRepositoryPort();
        useCase = new ListPublishedCoursesUseCase(repository);

        repository.addCourse(buildCourse("Atendimento de Excelência", "Hospitalidade", "INICIANTE", 5));
        repository.addCourse(buildCourse("Gestão de Reservas", "Turismo", "INTERMEDIÁRIO", 2));
        repository.addCourse(buildCourse("IA Aplicada ao Turismo", "Inteligência Artificial", "INICIANTE", 3));
    }

    @Test
    void shouldReturnAllCoursesWhenNoFilters() {
        final PageResult<CourseSummary> result = useCase.execute(null, null, null, null, null);

        assertEquals(3, result.totalElements());
        assertEquals(3, result.content().size());
        assertEquals(0, result.pageNumber());
        assertEquals(10, result.pageSize());
    }

    @Test
    void shouldFilterByTitleCaseInsensitive() {
        final PageResult<CourseSummary> result = useCase.execute("atendimento", null, null, null, null);

        assertEquals(1, result.totalElements());
        assertEquals("Atendimento de Excelência", result.content().get(0).title());
    }

    @Test
    void shouldFilterByTitlePartialMatch() {
        final PageResult<CourseSummary> result = useCase.execute("Turismo", null, null, null, null);

        assertEquals(1, result.totalElements());
        assertEquals("IA Aplicada ao Turismo", result.content().get(0).title());
    }

    @Test
    void shouldFilterByCategory() {
        final PageResult<CourseSummary> result = useCase.execute(null, "Hospitalidade", null, null, null);

        assertEquals(1, result.totalElements());
        assertEquals("Atendimento de Excelência", result.content().get(0).title());
    }

    @Test
    void shouldFilterByLevel() {
        final PageResult<CourseSummary> result = useCase.execute(null, null, "INICIANTE", null, null);

        assertEquals(2, result.totalElements());
    }

    @Test
    void shouldReturnEmptyWhenNoMatch() {
        final PageResult<CourseSummary> result = useCase.execute(null, "CategoriaInexistente", null, null, null);

        assertEquals(0, result.totalElements());
        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalPages());
    }

    @Test
    void shouldPaginateResults() {
        final PageResult<CourseSummary> firstPage = useCase.execute(null, null, null, 0, 2);

        assertEquals(3, firstPage.totalElements());
        assertEquals(2, firstPage.content().size());
        assertEquals(0, firstPage.pageNumber());
        assertEquals(2, firstPage.pageSize());
        assertEquals(2, firstPage.totalPages());

        final PageResult<CourseSummary> secondPage = useCase.execute(null, null, null, 1, 2);

        assertEquals(1, secondPage.content().size());
        assertEquals(1, secondPage.pageNumber());
    }

    @Test
    void shouldUseDefaultPageAndSizeWhenNull() {
        final PageResult<CourseSummary> result = useCase.execute(null, null, null, null, null);

        assertEquals(0, result.pageNumber());
        assertEquals(10, result.pageSize());
    }

    @Test
    void shouldClampNegativePageToZero() {
        final PageResult<CourseSummary> result = useCase.execute(null, null, null, -1, null);

        assertEquals(0, result.pageNumber());
    }

    @Test
    void shouldCapSizeAtMaximum() {
        final PageResult<CourseSummary> result = useCase.execute(null, null, null, null, 500);

        assertEquals(100, result.pageSize());
    }

    @Test
    void shouldCombineFilters() {
        final PageResult<CourseSummary> result = useCase.execute("IA", "Inteligência Artificial", "INICIANTE", null, null);

        assertEquals(1, result.totalElements());
        assertEquals("IA Aplicada ao Turismo", result.content().get(0).title());
    }

    private static CourseSummary buildCourse(String title, String category, String level, int lessonsCount) {
        return new CourseSummary(
                UUID.randomUUID(), title, "Descricao de " + title,
                category, level, 100, lessonsCount, 99.90,
                "https://cdn.escapa.com/thumb.jpg", "Instrutora Teste",
                4.5, 10);
    }
}
