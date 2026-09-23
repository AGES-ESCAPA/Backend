package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.domain.course.EnrollmentStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListStudentEnrollmentsUseCaseTest {

    @Test
    void shouldListStudentEnrollmentsWithFiltersAndPagination() {
        InMemoryEnrollmentRepositoryPort repository = new InMemoryEnrollmentRepositoryPort();
        ListStudentEnrollmentsUseCase useCase = new ListStudentEnrollmentsUseCase(repository);

        UUID userId = UUID.randomUUID();

        // Seed data
        repository.addCourseCard(new StudentCourseCard(UUID.randomUUID(), "Course A", "Inst 1", null, 10, 5, 100, EnrollmentStatus.COMPLETED));
        repository.addCourseCard(new StudentCourseCard(UUID.randomUUID(), "Course B", "Inst 2", null, 20, 10, 50, EnrollmentStatus.IN_PROGRESS));
        repository.addCourseCard(new StudentCourseCard(UUID.randomUUID(), "Course C", "Inst 3", null, 30, 15, 0, EnrollmentStatus.PENDING));

        // Act
        PageResult<StudentCourseCard> result = useCase.execute(userId, null, null, 0, 10);

        // Assert
        assertEquals(3, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(3, result.content().size());

        // Test filter by status
        PageResult<StudentCourseCard> completedResult = useCase.execute(userId, null, EnrollmentStatus.COMPLETED, 0, 10);
        assertEquals(1, completedResult.totalElements());
        assertEquals(EnrollmentStatus.COMPLETED, completedResult.content().get(0).enrollmentStatus());

        // Test filter by title
        PageResult<StudentCourseCard> searchResult = useCase.execute(userId, "course b", null, 0, 10);
        assertEquals(1, searchResult.totalElements());
        assertEquals("Course B", searchResult.content().get(0).title());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        InMemoryEnrollmentRepositoryPort repository = new InMemoryEnrollmentRepositoryPort();
        ListStudentEnrollmentsUseCase useCase = new ListStudentEnrollmentsUseCase(repository);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, null, 0, 10));
    }
}

