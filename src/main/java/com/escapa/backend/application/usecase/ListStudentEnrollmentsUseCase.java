package com.escapa.backend.application.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.application.port.CourseReviewRepositoryPort;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.domain.course.EnrollmentStatus;

public class ListStudentEnrollmentsUseCase {

    private final EnrollmentRepositoryPort enrollmentRepositoryPort;
    private final CourseReviewRepositoryPort courseReviewRepositoryPort;

    public ListStudentEnrollmentsUseCase(
            EnrollmentRepositoryPort enrollmentRepositoryPort,
            CourseReviewRepositoryPort courseReviewRepositoryPort
    ) {
        this.enrollmentRepositoryPort = enrollmentRepositoryPort;
        this.courseReviewRepositoryPort = courseReviewRepositoryPort;
    }

    public PageResult<StudentCourseCard> execute(UUID userId, String title, EnrollmentStatus status, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }

        final PageResult<StudentCourseCard> result =
                enrollmentRepositoryPort.findStudentEnrollments(userId, title, status, page, size);
        if (result.content().isEmpty()) {
            return result;
        }

        final List<UUID> courseIds = result.content().stream().map(StudentCourseCard::courseId).toList();
        final Map<UUID, Integer> ratings = courseReviewRepositoryPort.findRatingsByUser(userId, courseIds);
        final List<StudentCourseCard> content = result.content().stream()
                .map(card -> card.withMyRating(ratings.get(card.courseId())))
                .toList();
        return new PageResult<>(content, result.pageNumber(), result.pageSize(),
                result.totalElements(), result.totalPages());
    }
}
