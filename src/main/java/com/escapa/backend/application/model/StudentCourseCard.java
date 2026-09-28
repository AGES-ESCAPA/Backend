package com.escapa.backend.application.model;

import com.escapa.backend.domain.course.EnrollmentStatus;

import java.util.UUID;

/**
 * Modelo de leitura (Read Model) que consolida os dados de um curso
 * na visão do aluno (StudentCourseCard), carregado diretamente da base de dados.
 */
public record StudentCourseCard(
        UUID courseId,
        String title,
        String instructor,
        String thumbnailUrl,
        Integer durationTime,
        Integer lessonsCount,
        Integer progressPercentage,
        EnrollmentStatus enrollmentStatus
) {
}

