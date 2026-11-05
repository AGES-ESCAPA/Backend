package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CourseEnrollmentCount;
import com.escapa.backend.application.model.CourseMetrics;
import com.escapa.backend.application.model.CourseMetrics.CategoryShare;
import com.escapa.backend.application.model.CourseMetrics.CourseRanking;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula as métricas históricas de matrículas por curso e por categoria (US-37).
 */
public class GetAdminCourseMetricsUseCase {

    private static final int MAX_TOP_COURSES = 5;
    private static final long TOTAL_BASIS_POINTS = 10000L;
    private static final int PERCENTAGE_SCALE = 2;
    private static final String UNCATEGORIZED_LABEL = "Sem categoria";

    private static final Comparator<CourseEnrollmentCount> COURSE_RANKING_COMPARATOR =
            Comparator.comparingLong(CourseEnrollmentCount::enrollments).reversed()
                    .thenComparing(course -> course.courseId().toString());

    private static final Comparator<CategoryRemainder> REMAINDER_COMPARATOR =
            Comparator.comparingLong(CategoryRemainder::remainderNumerator).reversed()
                    .thenComparing(CategoryRemainder::category);

    private static final Comparator<CategoryShare> CATEGORY_SHARE_COMPARATOR =
            Comparator.comparing(CategoryShare::percentage).reversed()
                    .thenComparing(CategoryShare::category);

    private final EnrollmentRepositoryPort enrollmentRepositoryPort;

    public GetAdminCourseMetricsUseCase(EnrollmentRepositoryPort enrollmentRepositoryPort) {
        this.enrollmentRepositoryPort = enrollmentRepositoryPort;
    }

    public CourseMetrics execute() {
        final List<CourseEnrollmentCount> eligibleCourses = enrollmentRepositoryPort
                .countEligibleEnrollmentsByCourse().stream()
                .filter(course -> course.enrollments() > 0)
                .toList();

        if (eligibleCourses.isEmpty()) {
            return new CourseMetrics(null, List.of(), 0, List.of());
        }

        final List<CourseRanking> topCourses = buildTopCourses(eligibleCourses);
        final List<CategoryShare> enrollmentsByCategory = buildCategoryShares(eligibleCourses);

        return new CourseMetrics(
                topCourses.get(0),
                topCourses,
                enrollmentsByCategory.size(),
                enrollmentsByCategory
        );
    }

    private List<CourseRanking> buildTopCourses(List<CourseEnrollmentCount> eligibleCourses) {
        return eligibleCourses.stream()
                .sorted(COURSE_RANKING_COMPARATOR)
                .limit(MAX_TOP_COURSES)
                .map(course -> new CourseRanking(course.courseId(), course.title(), course.enrollments()))
                .toList();
    }

    private List<CategoryShare> buildCategoryShares(List<CourseEnrollmentCount> eligibleCourses) {
        final Map<String, Long> countsByCategory = new HashMap<>();
        long totalEnrollments = 0L;
        for (final CourseEnrollmentCount course : eligibleCourses) {
            final String category = normalizeCategory(course.category());
            countsByCategory.merge(category, course.enrollments(), Long::sum);
            totalEnrollments += course.enrollments();
        }
        return distributePercentages(countsByCategory, totalEnrollments);
    }

    private List<CategoryShare> distributePercentages(Map<String, Long> countsByCategory, long totalEnrollments) {
        final List<CategoryRemainder> remainders = new ArrayList<>();
        long sumFloorBasisPoints = 0L;
        for (final Map.Entry<String, Long> entry : countsByCategory.entrySet()) {
            final long scaled = entry.getValue() * TOTAL_BASIS_POINTS;
            final long floorBasisPoints = scaled / totalEnrollments;
            final long remainderNumerator = scaled % totalEnrollments;
            sumFloorBasisPoints += floorBasisPoints;
            remainders.add(new CategoryRemainder(entry.getKey(), floorBasisPoints, remainderNumerator));
        }

        remainders.sort(REMAINDER_COMPARATOR);
        final int extraPoints = (int) (TOTAL_BASIS_POINTS - sumFloorBasisPoints);
        final List<CategoryShare> shares = new ArrayList<>(remainders.size());
        for (int index = 0; index < remainders.size(); index++) {
            final CategoryRemainder item = remainders.get(index);
            final long basisPoints = item.floorBasisPoints() + (index < extraPoints ? 1L : 0L);
            shares.add(new CategoryShare(item.category(), BigDecimal.valueOf(basisPoints, PERCENTAGE_SCALE)));
        }
        shares.sort(CATEGORY_SHARE_COMPARATOR);
        return List.copyOf(shares);
    }

    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return UNCATEGORIZED_LABEL;
        }
        return category.trim();
    }

    private record CategoryRemainder(
            String category,
            long floorBasisPoints,
            long remainderNumerator
    ) {
    }
}

