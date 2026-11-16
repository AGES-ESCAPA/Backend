package com.escapa.backend.application.usecase;

import com.escapa.backend.application.course.PublishedCurriculum;
import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.application.port.LessonNotificationPort;
import com.escapa.backend.domain.content.ContentPendingDraft;
import com.escapa.backend.domain.content.ContentNotFoundException;
import com.escapa.backend.domain.content.ContentResource;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.ContentTypeRules;
import com.escapa.backend.domain.entity.Content;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Edita uma aula. Em curso publicado, a versão que o aluno lê só muda na
 * publicação. Fora isso, quem já concluiu a aula é avisado na hora.
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
        final Content prospective = prospective(content, title, type, url, durationMinutes, description, isFree,
                resources);
        ContentTypeRules.validate(prospective);

        if (content.isHeldForPublish()) {
            content.setPendingDraft(ContentPendingDraft.from(prospective));
            return PublishedCurriculum.editorContent(contentRepositoryPort.save(content));
        }

        apply(content, prospective);
        final Content saved = contentRepositoryPort.save(content);
        if (!before.equals(EditableFields.of(saved))) {
            lessonNotificationPort.notifyLessonUpdated(saved.getId(), saved.getTitle());
        }
        return saved;
    }

    private static Content prospective(
            Content current,
            String title,
            ContentType type,
            String url,
            Integer durationMinutes,
            String description,
            Boolean isFree,
            List<ContentResource> resources
    ) {
        final Content next = current.copy();
        next.setTitle(title.trim());
        next.setType(type);
        next.setUrl(url);
        next.setDurationMinutes(durationMinutes);
        next.setDescription(description);
        next.setIsFree(isFree != null && isFree);
        next.setResources(resources == null ? new ArrayList<>() : new ArrayList<>(resources));
        next.setPendingDraft(null);
        return next;
    }

    private static void apply(Content content, Content prospective) {
        content.setTitle(prospective.getTitle());
        content.setType(prospective.getType());
        content.setUrl(prospective.getUrl());
        content.setDurationMinutes(prospective.getDurationMinutes());
        content.setDescription(prospective.getDescription());
        content.setIsFree(prospective.getIsFree());
        content.setResources(prospective.getResources());
        content.setPendingDraft(null);
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
