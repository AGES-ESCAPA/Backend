package com.escapa.backend.application.model;

import java.util.UUID;

/**
 * Curso da empresa na visão do colaborador: se a matrícula corporativa está ativa.
 */
public record EmployeeCourseAssignment(
        UUID courseId,
        String title,
        String category,
        boolean assigned
) {
}
