package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;

import java.time.LocalDateTime;

public final class ContentMapper {

    private ContentMapper() {
    }

    public static Content toDomain(ContentEntity entity) {
        if (entity == null) {
            return null;
        }
        final Content content = new Content();
        content.setId(entity.getId());
        content.setModuleId(entity.getModule() != null ? entity.getModule().getId() : null);
        content.setTitle(entity.getTitle());
        content.setDescription(entity.getDescription());
        content.setType(entity.getType());
        content.setUrl(entity.getUrl());
        content.setDurationMinutes(entity.getDurationMinutes());
        content.setIsFree(entity.getIsFree());
        content.setOrder(entity.getOrder());
        content.setResources(entity.getRecursos());
        content.setCreatedAt(entity.getCreatedAt());
        return content;
    }

    /**
     * Copia os campos editaveis para a entidade. {@code recursos} fica de fora
     * enquanto o preenchimento dele nao for definido pelo produto.
     */
    public static void applyToEntity(Content content, ContentEntity entity, ModuleEntity module) {
        entity.setModule(module);
        entity.setTitle(content.getTitle());
        entity.setDescription(content.getDescription());
        entity.setType(content.getType());
        entity.setUrl(content.getUrl());
        entity.setDurationMinutes(content.getDurationMinutes());
        entity.setIsFree(content.getIsFree() != null && content.getIsFree());
        entity.setOrder(content.getOrder());
        if (entity.getCreatedAt() == null) {
            final LocalDateTime createdAt =
                    content.getCreatedAt() != null ? content.getCreatedAt() : LocalDateTime.now();
            entity.setCreatedAt(createdAt);
        }
    }
}
