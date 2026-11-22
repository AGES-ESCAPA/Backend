package com.escapa.backend.adapters.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.escapa.backend.domain.content.ContentResource;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.QuizQuestion;
import com.escapa.backend.domain.entity.Content;

public record ContentResponse(
        UUID id,
        UUID moduleId,
        String title,
        ContentType type,
        String url,
        Integer durationMinutes,
        String description,
        Boolean isFree,
        Integer order,
        List<ContentResource> resources,
        List<QuizQuestion> quizQuestions,
        LocalDateTime createdAt
) {
    public static ContentResponse from(Content content) {
        return new ContentResponse(
                content.getId(),
                content.getModuleId(),
                content.getTitle(),
                content.getType(),
                content.getUrl(),
                content.getDurationMinutes(),
                content.getDescription(),
                content.getIsFree(),
                content.getOrder(),
                content.getResources(),
                content.getQuizQuestions(),
                content.getCreatedAt()
        );
    }
}
