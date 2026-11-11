package com.escapa.backend.domain.content;

import com.escapa.backend.domain.entity.Content;

import java.util.ArrayList;
import java.util.List;

/**
 * Edição de uma aula que os alunos já veem. As colunas publicadas permanecem
 * até o curso ser publicado de novo.
 */
public record ContentPendingDraft(
        String title,
        String description,
        ContentType type,
        String url,
        Integer durationMinutes,
        Boolean isFree,
        List<ContentResource> resources) {

    public ContentPendingDraft {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }

    public static ContentPendingDraft from(Content content) {
        return new ContentPendingDraft(
                content.getTitle(),
                content.getDescription(),
                content.getType(),
                content.getUrl(),
                content.getDurationMinutes(),
                content.getIsFree(),
                content.getResources());
    }

    public boolean differsFrom(Content published) {
        return !this.equals(from(published));
    }

    public void applyTo(Content content) {
        content.setTitle(title);
        content.setDescription(description);
        content.setType(type);
        content.setUrl(url);
        content.setDurationMinutes(durationMinutes);
        content.setIsFree(isFree != null && isFree);
        content.setResources(new ArrayList<>(resources));
    }
}
