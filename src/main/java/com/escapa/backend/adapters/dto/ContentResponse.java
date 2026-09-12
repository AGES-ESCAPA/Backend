package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;

import java.time.LocalDateTime;
import java.util.UUID;

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
        String resources,
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
                content.getCreatedAt()
        );
    }
}
