package com.escapa.backend.application.model;

import com.escapa.backend.domain.course.EnrollmentStatus;

import java.util.UUID;

/**
 * Modelo de leitura (Read Model) que consolida os dados de um curso
 * na visão do aluno (StudentCourseCard), carregado diretamente da base de dados.
 * {@code myRating} e a nota (1 a 5) que o aluno deu ao curso, ou null se ainda nao avaliou.
 */
public record StudentCourseCard(
        UUID courseId,
        String title,
        String instructor,
        String thumbnailUrl,
        Integer durationTime,
        Integer lessonsCount,
        Integer progressPercentage,
        EnrollmentStatus enrollmentStatus,
        String certificateCode,
        Integer myRating
) {
    public StudentCourseCard(
            UUID courseId,
            String title,
            String instructor,
            String thumbnailUrl,
            Integer durationTime,
            Integer lessonsCount,
            Integer progressPercentage,
            EnrollmentStatus enrollmentStatus,
            String certificateCode
    ) {
        this(courseId, title, instructor, thumbnailUrl, durationTime, lessonsCount,
                progressPercentage, enrollmentStatus, certificateCode, null);
    }

    public StudentCourseCard(
            UUID courseId,
            String title,
            String instructor,
            String thumbnailUrl,
            Integer durationTime,
            Integer lessonsCount,
            Integer progressPercentage,
            EnrollmentStatus enrollmentStatus
    ) {
        this(courseId, title, instructor, thumbnailUrl, durationTime, lessonsCount,
                progressPercentage, enrollmentStatus, null, null);
    }

    public StudentCourseCard withMyRating(Integer rating) {
        return new StudentCourseCard(courseId, title, instructor, thumbnailUrl, durationTime, lessonsCount,
                progressPercentage, enrollmentStatus, certificateCode, rating);
    }
}
