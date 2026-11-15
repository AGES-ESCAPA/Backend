package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.CourseMetrics;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CourseMetricsResponse(
        CourseRankingResponse mostAccessedCourse,
        List<CourseRankingResponse> topCourses,
        int categoriesCount,
        List<CategoryEnrollmentResponse> enrollmentsByCategory
) {

    public record CourseRankingResponse(
            UUID courseId,
            String title,
            long enrollments
    ) {
        static CourseRankingResponse from(CourseMetrics.CourseRanking ranking) {
            if (ranking == null) {
                return null;
            }
            return new CourseRankingResponse(ranking.courseId(), ranking.title(), ranking.enrollments());
        }
    }

    public record CategoryEnrollmentResponse(
            String category,
            BigDecimal percentage
    ) {
        static CategoryEnrollmentResponse from(CourseMetrics.CategoryShare share) {
            return new CategoryEnrollmentResponse(share.category(), share.percentage());
        }
    }

    public static CourseMetricsResponse from(CourseMetrics metrics) {
        return new CourseMetricsResponse(
                CourseRankingResponse.from(metrics.mostAccessedCourse()),
                metrics.topCourses().stream().map(CourseRankingResponse::from).toList(),
                metrics.categoriesCount(),
                metrics.enrollmentsByCategory().stream().map(CategoryEnrollmentResponse::from).toList()
        );
    }
}

