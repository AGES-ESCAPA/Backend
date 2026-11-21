package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.application.port.CourseReviewRepositoryPort;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.domain.course.CourseReviewNotAllowedException;

/**
 * Registra a avaliacao (1 a 5 estrelas) que o aluno da a um curso que concluiu.
 * Avaliar de novo substitui a nota anterior: ha uma avaliacao por aluno por curso.
 */
public class RateCourseUseCase {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    private final EnrollmentRepositoryPort enrollmentRepositoryPort;
    private final CourseReviewRepositoryPort courseReviewRepositoryPort;
    private final Clock clock;

    public RateCourseUseCase(
            EnrollmentRepositoryPort enrollmentRepositoryPort,
            CourseReviewRepositoryPort courseReviewRepositoryPort,
            Clock clock
    ) {
        this.enrollmentRepositoryPort = enrollmentRepositoryPort;
        this.courseReviewRepositoryPort = courseReviewRepositoryPort;
        this.clock = clock;
    }

    public int execute(UUID userId, UUID courseId, int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("Rating must be between " + MIN_RATING + " and " + MAX_RATING);
        }
        if (!enrollmentRepositoryPort.hasCompletedCourse(userId, courseId)) {
            throw new CourseReviewNotAllowedException();
        }

        courseReviewRepositoryPort.saveRating(userId, courseId, rating, LocalDateTime.now(clock));
        return rating;
    }
}
