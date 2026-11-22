package com.escapa.backend.domain.content;

import java.util.ArrayList;
import java.util.List;

import com.escapa.backend.domain.entity.Content;

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
        List<ContentResource> resources,
        List<QuizQuestion> quizQuestions) {

    public ContentPendingDraft {
        resources = resources == null ? List.of() : List.copyOf(resources);
        quizQuestions = quizQuestions == null ? List.of() : List.copyOf(quizQuestions);
    }

    public static ContentPendingDraft from(Content content) {
        return new ContentPendingDraft(
                content.getTitle(),
                content.getDescription(),
                content.getType(),
                content.getUrl(),
                content.getDurationMinutes(),
                content.getIsFree(),
                content.getResources(),
                content.getQuizQuestions());
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
        content.setQuizQuestions(new ArrayList<>(quizQuestions));
    }
}