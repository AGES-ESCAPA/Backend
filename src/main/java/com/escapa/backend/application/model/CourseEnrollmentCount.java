package com.escapa.backend.application.model;

import java.util.UUID;

/**
 * Contagem agregada de matrículas elegíveis de um curso para cálculo das métricas
 * históricas do catálogo (US-37).
 */
public record CourseEnrollmentCount(
        UUID courseId,
        String title,
        String category,
        long enrollments
) {
}

