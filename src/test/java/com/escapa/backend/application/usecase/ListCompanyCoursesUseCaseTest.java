package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.CompanyCourseSummary;
import com.escapa.backend.application.port.CompanyCourseRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListCompanyCoursesUseCaseTest {

    @Test
    void shouldReturnCoursesWithEmployeesWithAccess() {
        final CompanyCourseSummary course = new CompanyCourseSummary(
                UUID.randomUUID(),
                "Curso",
                "Descrição",
                "thumbnail.jpg",
                "Categoria",
                "INICIANTE",
                120,
                10,
                "Instrutor",
                4.5,
                LocalDate.of(2026, 11, 1),
                3L);

        final ListCompanyCoursesUseCase useCase =
                new ListCompanyCoursesUseCase(new StubRepository(List.of(course)));

        final List<CompanyCourseSummary> result =
                useCase.execute(UUID.randomUUID());

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).employeesWithAccess());
    }

    @Test
    void shouldReturnEmptyListWhenCompanyHasNoCourses() {
        final ListCompanyCoursesUseCase useCase =
                new ListCompanyCoursesUseCase(new StubRepository(List.of()));

        assertEquals(List.of(), useCase.execute(UUID.randomUUID()));
    }

    @Test
    void shouldRejectNullCompanyId() {
        final ListCompanyCoursesUseCase useCase =
                new ListCompanyCoursesUseCase(new StubRepository(List.of()));

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
    }

    private record StubRepository(List<CompanyCourseSummary> courses)
            implements CompanyCourseRepositoryPort {

        @Override
        public List<UUID> findAcquiredCourseIds(UUID companyId) {
            return List.of();
        }

        @Override
        public List<CompanyCourseSummary> findActiveCourses(UUID companyId) {
            return courses;
        }
    }
}