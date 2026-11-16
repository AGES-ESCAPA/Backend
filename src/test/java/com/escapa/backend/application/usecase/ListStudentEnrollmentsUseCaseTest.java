package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.domain.course.EnrollmentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListStudentEnrollmentsUseCaseTest {

    private final InMemoryEnrollmentRepositoryPort repository = new InMemoryEnrollmentRepositoryPort();
    private final InMemoryCourseReviewRepositoryPort reviewRepository = new InMemoryCourseReviewRepositoryPort();
    private final ListStudentEnrollmentsUseCase useCase = new ListStudentEnrollmentsUseCase(repository, reviewRepository);

    @Test
    void shouldListStudentEnrollmentsWithFiltersAndPagination() {
        UUID userId = UUID.randomUUID();
        repository.addCourseCard(new StudentCourseCard(UUID.randomUUID(), "Course A", "Inst 1", null, 10, 5, 100, EnrollmentStatus.COMPLETED));
        repository.addCourseCard(new StudentCourseCard(UUID.randomUUID(), "Course B", "Inst 2", null, 20, 10, 50, EnrollmentStatus.IN_PROGRESS));
        repository.addCourseCard(new StudentCourseCard(UUID.randomUUID(), "Course C", "Inst 3", null, 30, 15, 0, EnrollmentStatus.PENDING));
        PageResult<StudentCourseCard> result = useCase.execute(userId, null, null, 0, 10);
        assertEquals(3, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(3, result.content().size());
        PageResult<StudentCourseCard> completedResult = useCase.execute(userId, null, EnrollmentStatus.COMPLETED, 0, 10);
        assertEquals(1, completedResult.totalElements());
        assertEquals(EnrollmentStatus.COMPLETED, completedResult.content().get(0).enrollmentStatus());
        PageResult<StudentCourseCard> searchResult = useCase.execute(userId, "course b", null, 0, 10);
        assertEquals(1, searchResult.totalElements());
        assertEquals("Course B", searchResult.content().get(0).title());
    }

    @Test
    void shouldFillTheRatingTheStudentGaveToEachCourse() {
        final UUID userId = UUID.randomUUID();
        final UUID ratedCourseId = UUID.randomUUID();
        final UUID unratedCourseId = UUID.randomUUID();
        repository.addCourseCard(new StudentCourseCard(ratedCourseId, "Rated", "Inst", null, 10, 5, 100, EnrollmentStatus.COMPLETED));
        repository.addCourseCard(new StudentCourseCard(unratedCourseId, "Unrated", "Inst", null, 10, 5, 100, EnrollmentStatus.COMPLETED));
        reviewRepository.saveRating(userId, ratedCourseId, 4, LocalDateTime.now());
        reviewRepository.saveRating(UUID.randomUUID(), unratedCourseId, 2, LocalDateTime.now());

        final PageResult<StudentCourseCard> result = useCase.execute(userId, null, null, 0, 10);

        assertEquals(4, result.content().get(0).myRating());
        assertNull(result.content().get(1).myRating());
        assertEquals(2, result.totalElements());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, null, 0, 10));
    }
}
