package com.escapa.backend.adapters.dto;

import java.util.List;
import java.util.UUID;

import com.escapa.backend.application.model.StudentCourseCurriculum;
import com.escapa.backend.domain.content.LessonStatus;

/** Grade de aulas do curso para o aluno (US-13), com o status de cada aula. */
public record StudentCourseCurriculumResponse(
        UUID courseId,
        int completedLessons,
        int totalLessons,
        List<ModuleResponse> modules
) {
    public record ModuleResponse(
            UUID moduleId,
            String title,
            Integer order,
            boolean locked,
            int completedLessons,
            int totalLessons,
            List<LessonResponse> lessons
    ) {
    }

    public record LessonResponse(
            UUID lessonId,
            String title,
            Integer order,
            String type,
            Integer durationMinutes,
            LessonStatus status
    ) {
    }

    public static StudentCourseCurriculumResponse from(StudentCourseCurriculum curriculum) {
        final List<ModuleResponse> modules = curriculum.modules().stream()
                .map(StudentCourseCurriculumResponse::toModuleResponse)
                .toList();
        return new StudentCourseCurriculumResponse(
                curriculum.courseId(), curriculum.completedLessons(), curriculum.totalLessons(), modules);
    }

    private static ModuleResponse toModuleResponse(StudentCourseCurriculum.Module module) {
        final List<LessonResponse> lessons = module.lessons().stream()
                .map(StudentCourseCurriculumResponse::toLessonResponse)
                .toList();
        return new ModuleResponse(
                module.moduleId(), module.title(), module.order(), module.locked(),
                module.completedLessons(), module.totalLessons(), lessons);
    }

    private static LessonResponse toLessonResponse(StudentCourseCurriculum.Lesson lesson) {
        return new LessonResponse(
                lesson.lessonId(), lesson.title(), lesson.order(), lesson.type(),
                lesson.durationMinutes(), lesson.status());
    }
}