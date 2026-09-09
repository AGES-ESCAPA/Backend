package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.ContentTypeRules;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.module.ModuleNotFoundException;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreateContentUseCase {

    private final ContentRepositoryPort contentRepositoryPort;
    private final ModuleRepositoryPort moduleRepositoryPort;

    public CreateContentUseCase(
            ContentRepositoryPort contentRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort
    ) {
        this.contentRepositoryPort = contentRepositoryPort;
        this.moduleRepositoryPort = moduleRepositoryPort;
    }

    public Content execute(
            UUID moduleId,
            String title,
            ContentType type,
            String url,
            Integer durationMinutes,
            String description,
            Boolean isFree
    ) {
        if (moduleId == null) {
            throw new IllegalArgumentException("moduleId is required");
        }
        if (!moduleRepositoryPort.existsById(moduleId)) {
            throw new ModuleNotFoundException(moduleId);
        }
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }

        final Content content = new Content();
        content.setModuleId(moduleId);
        content.setTitle(title.trim());
        content.setType(type);
        content.setUrl(url);
        content.setDurationMinutes(durationMinutes);
        content.setDescription(description);
        content.setIsFree(isFree != null && isFree);
        content.setOrder(contentRepositoryPort.nextOrder(moduleId));
        content.setCreatedAt(LocalDateTime.now());

        ContentTypeRules.validate(content);
        return contentRepositoryPort.save(content);
    }
}
