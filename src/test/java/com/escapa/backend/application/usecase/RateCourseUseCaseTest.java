package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.domain.course.CourseReviewNotAllowedException;

class RateCourseUseCaseTest {

    private final InMemoryEnrollmentRepositoryPort enrollmentRepository = new InMemoryEnrollmentRepositoryPort();
    private final InMemoryCourseReviewRepositoryPort reviewRepository = new InMemoryCourseReviewRepositoryPort();
    private final RateCourseUseCase useCase =
            new RateCourseUseCase(enrollmentRepository, reviewRepository, Clock.systemUTC());

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    private void givenEnrollment(boolean completed) {
        enrollmentRepository.enroll(userId, courseId, new Enrollment(LocalDate.now().minusDays(30), null));
        if (completed) {
            enrollmentRepository.complete(userId, courseId);
        }
    }

    @Test
    void shouldSaveRatingOfCompletedCourse() {
        givenEnrollment(true);

        final int rating = useCase.execute(userId, courseId, 4);

        assertEquals(4, rating);
        assertEquals(4, reviewRepository.ratingOf(userId, courseId));
    }

    @Test
    void shouldReplacePreviousRating() {
        givenEnrollment(true);
        useCase.execute(userId, courseId, 2);

        useCase.execute(userId, courseId, 5);

        assertEquals(5, reviewRepository.ratingOf(userId, courseId));
    }

    @Test
    void shouldRejectRatingOfCourseNotCompleted() {
        givenEnrollment(false);

        assertThrows(CourseReviewNotAllowedException.class, () -> useCase.execute(userId, courseId, 5));
        assertNull(reviewRepository.ratingOf(userId, courseId));
    }

    @Test
    void shouldRejectRatingWithoutEnrollment() {
        assertThrows(CourseReviewNotAllowedException.class, () -> useCase.execute(userId, courseId, 5));
    }

    @Test
    void shouldRejectRatingOfRevokedEnrollment() {
        givenEnrollment(true);
        enrollmentRepository.revoke(userId, courseId);

        assertThrows(CourseReviewNotAllowedException.class, () -> useCase.execute(userId, courseId, 5));
    }

    @Test
    void shouldRejectRatingOutOfRange() {
        givenEnrollment(true);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(userId, courseId, 0));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(userId, courseId, 6));
        assertNull(reviewRepository.ratingOf(userId, courseId));
    }
}
