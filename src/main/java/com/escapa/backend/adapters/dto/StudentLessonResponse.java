package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;

import java.util.UUID;

/**
 * Aula para o player e o cabecalho da Sala de Aula. {@code description} ja vem
 * preenchida para a US-12 reaproveitar o mesmo endpoint.
 */
public record StudentLessonResponse(
        UUID id,
        UUID courseId,
        String title,
        String description,
        ContentType type,
        String url,
        Integer durationMinutes,
        Boolean isFree,
        Integer order,
        String resources,
        ModuleInfo module
) {
    public record ModuleInfo(UUID id, String title, Integer order) {
    }

    public static StudentLessonResponse from(LessonDetails lesson) {
        final Content content = lesson.content();
        return new StudentLessonResponse(
                content.getId(),
                lesson.courseId(),
                content.getTitle(),
                content.getDescription(),
                content.getType(),
                content.getUrl(),
                content.getDurationMinutes(),
                content.getIsFree(),
                content.getOrder(),
                content.getResources(),
                new ModuleInfo(content.getModuleId(), lesson.moduleTitle(), lesson.moduleOrder())
        );
    }
}
