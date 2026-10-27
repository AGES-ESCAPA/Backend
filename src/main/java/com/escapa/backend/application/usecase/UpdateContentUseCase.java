package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.application.port.LessonNotificationPort;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentResource;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.ContentTypeRules;
import com.escapa.backend.domain.entity.Content;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Edita uma aula existente. Quando algum campo muda de fato, os alunos que ja
 * concluiram a aula sao notificados para refaze-la.
 */
public class UpdateContentUseCase {

    private final ContentRepositoryPort contentRepositoryPort;
    private final LessonNotificationPort lessonNotificationPort;

    public UpdateContentUseCase(
            ContentRepositoryPort contentRepositoryPort,
            LessonNotificationPort lessonNotificationPort
    ) {
        this.contentRepositoryPort = contentRepositoryPort;
        this.lessonNotificationPort = lessonNotificationPort;
    }

    public Content execute(
            UUID id,
            String title,
            ContentType type,
            String url,
            Integer durationMinutes,
            String description,
            Boolean isFree
    ) {
        return execute(id, title, type, url, durationMinutes, description, isFree, List.of());
    }

    public Content execute(
            UUID id,
            String title,
            ContentType type,
            String url,
            Integer durationMinutes,
            String description,
            Boolean isFree,
            List<ContentResource> resources
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }

        final Content content = contentRepositoryPort.findById(id)
                .orElseThrow(() -> new ContentNotFoundException(id));
        final EditableFields before = EditableFields.of(content);

        // Trocar o tipo e limpar campos do tipo anterior e permitido; a validacao
        // condicional abaixo garante que a nova combinacao continue coerente.
        content.setTitle(title.trim());
        content.setType(type);
        content.setUrl(url);
        content.setDurationMinutes(durationMinutes);
        content.setDescription(description);
        content.setIsFree(isFree != null && isFree);
        content.setResources(resources == null ? new ArrayList<>() : new ArrayList<>(resources));

        ContentTypeRules.validate(content);
        final Content saved = contentRepositoryPort.save(content);

        if (!before.equals(EditableFields.of(saved))) {
            lessonNotificationPort.notifyLessonUpdated(saved.getId(), saved.getTitle());
        }
        return saved;
    }

    private record EditableFields(
            String title,
            ContentType type,
            String url,
            Integer durationMinutes,
            String description,
            Boolean isFree,
            List<ContentResource> resources
    ) {
        static EditableFields of(Content content) {
            return new EditableFields(
                    content.getTitle(),
                    content.getType(),
                    content.getUrl(),
                    content.getDurationMinutes(),
                    content.getDescription(),
                    content.getIsFree(),
                    content.getResources() == null ? List.of() : List.copyOf(content.getResources()));
        }
    }
}
