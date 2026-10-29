package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.content.ContentResource;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ContentMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
        content.setResources(readResources(entity.getRecursos()));
        content.setCreatedAt(entity.getCreatedAt());
        return content;
    }

    public static void applyToEntity(Content content, ContentEntity entity, ModuleEntity module) {
        entity.setModule(module);
        entity.setTitle(content.getTitle());
        entity.setDescription(content.getDescription());
        entity.setType(content.getType());
        entity.setUrl(content.getUrl());
        entity.setDurationMinutes(content.getDurationMinutes());
        entity.setIsFree(content.getIsFree() != null && content.getIsFree());
        entity.setOrder(content.getOrder());
        entity.setRecursos(writeResources(content.getResources()));
        if (entity.getCreatedAt() == null) {
            final LocalDateTime createdAt =
                    content.getCreatedAt() != null ? content.getCreatedAt() : LocalDateTime.now();
            entity.setCreatedAt(createdAt);
        }
    }

    private static List<ContentResource> readResources(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(OBJECT_MAPPER.readValue(json, new TypeReference<List<ContentResource>>() { }));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private static String writeResources(List<ContentResource> resources) {
        if (resources == null || resources.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(resources);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize content resources", e);
        }
    }
}
