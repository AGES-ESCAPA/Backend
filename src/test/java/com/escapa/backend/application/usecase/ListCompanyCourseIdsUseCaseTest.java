package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.CompanyCourseSummary;
import com.escapa.backend.application.port.CompanyCourseRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListCompanyCourseIdsUseCaseTest {

    @Test
    void shouldReturnTheCoursesOfTheCompany() {
        final UUID companyId = UUID.randomUUID();
        final UUID courseId = UUID.randomUUID();

        final CompanyCourseRepositoryPort repository =
                new CompanyCourseRepositoryPort() {
                    @Override
                    public List<UUID> findAcquiredCourseIds(UUID id) {
                        return id.equals(companyId) ? List.of(courseId) : List.of();
                    }

                    @Override
                    public List<CompanyCourseSummary> findActiveCourses(UUID id) {
                        return List.of();
                    }
                };

        final ListCompanyCourseIdsUseCase useCase =
                new ListCompanyCourseIdsUseCase(repository);

        assertEquals(List.of(courseId), useCase.execute(companyId));
        assertEquals(List.of(), useCase.execute(UUID.randomUUID()));
    }

    @Test
    void shouldThrowExceptionWhenCompanyIdIsNull() {
        final CompanyCourseRepositoryPort repository =
                new CompanyCourseRepositoryPort() {
                    @Override
                    public List<UUID> findAcquiredCourseIds(UUID id) {
                        return List.of();
                    }

                    @Override
                    public List<CompanyCourseSummary> findActiveCourses(UUID id) {
                        return List.of();
                    }
                };

        final ListCompanyCourseIdsUseCase useCase =
                new ListCompanyCourseIdsUseCase(repository);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
    }
}