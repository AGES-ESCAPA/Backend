package com.escapa.backend.application.model;

import com.escapa.backend.domain.content.LessonStatus;

import java.util.List;
import java.util.UUID;

/** Grade de aulas de um curso para o aluno (US-13): modulos, aulas e o status de cada uma. */
public record StudentCourseCurriculum(
        UUID courseId,
        int completedLessons,
        int totalLessons,
        List<Module> modules
) {

    public record Module(
            UUID moduleId,
            String title,
            Integer order,
            boolean locked,
            int completedLessons,
            int totalLessons,
            List<Lesson> lessons
    ) {
    }

    public record Lesson(
            UUID lessonId,
            String title,
            Integer order,
            String type,
            Integer durationMinutes,
            LessonStatus status
    ) {
    }
}
