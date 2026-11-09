package com.escapa.backend.application.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Métricas históricas de adesão ao catálogo para o painel administrativo (US-37):
 * curso mais acessado (com mais matrículas), top 5 cursos e distribuição percentual
 * das matrículas elegíveis por categoria.
 */
public record CourseMetrics(
        CourseRanking mostAccessedCourse,
        List<CourseRanking> topCourses,
        int categoriesCount,
        List<CategoryShare> enrollmentsByCategory
) {

    public record CourseRanking(
            UUID courseId,
            String title,
            long enrollments
    ) {
    }

    public record CategoryShare(
            String category,
            BigDecimal percentage
    ) {
    }
}

