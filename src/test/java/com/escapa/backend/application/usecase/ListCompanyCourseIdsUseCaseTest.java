package com.escapa.backend.application.usecase;

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
        final ListCompanyCourseIdsUseCase useCase = new ListCompanyCourseIdsUseCase(
                id -> id.equals(companyId) ? List.of(courseId) : List.of());

        assertEquals(List.of(courseId), useCase.execute(companyId));
        assertEquals(List.of(), useCase.execute(UUID.randomUUID()));
    }

    @Test
    void shouldThrowExceptionWhenCompanyIdIsNull() {
        final ListCompanyCourseIdsUseCase useCase = new ListCompanyCourseIdsUseCase(id -> List.of());

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
    }
}
