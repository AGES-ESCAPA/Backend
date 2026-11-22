package com.escapa.backend.domain.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.content.QuizOption;
import com.escapa.backend.domain.content.QuizQuestion;
import com.escapa.backend.domain.entity.Content;

class ContentPendingDraftTest {

    @Test
    void shouldCopyResourcesAndTreatMissingResourcesAsAnEmptyList() {
        final ContentResource resource = new ContentResource("resource-1", "link", "Guia", "https://example.com");
        final List<ContentResource> resources = new ArrayList<>(List.of(resource));
        final ContentPendingDraft draft = new ContentPendingDraft(
                "Aula", "Descrição", ContentType.TEXT, null, null, null, resources, List.of());

        resources.clear();

        assertEquals(List.of(resource), draft.resources());
        assertThrows(UnsupportedOperationException.class, () -> draft.resources().add(resource));
        assertEquals(List.of(), new ContentPendingDraft(
                "Aula", null, ContentType.TEXT, null, null, null, null, List.of()).resources());
    }

    @Test
    void shouldDetectWhetherTheDraftDiffersFromPublishedContent() {
        final Content content = content("Aula publicada", "Descrição", true);
        final ContentPendingDraft matchingDraft = ContentPendingDraft.from(content);
        final ContentPendingDraft changedDraft = new ContentPendingDraft(
                "Aula revisada", "Descrição", ContentType.TEXT, null, null, true, List.of(), List.of());

        assertFalse(matchingDraft.differsFrom(content));
        assertTrue(changedDraft.differsFrom(content));
        assertEquals("Aula publicada", matchingDraft.title());
    }

    @Test
    void shouldCopyQuizQuestionsWhenApplyingDraft() {
        final Content content = content("Aula", "Descrição", false);
        final List<QuizQuestion> questions = List.of(new QuizQuestion(
                UUID.randomUUID(),
                "Pergunta",
                List.of(
                        new QuizOption(UUID.randomUUID(), "A", true),
                        new QuizOption(UUID.randomUUID(), "B", false))));
        final ContentPendingDraft draft = new ContentPendingDraft(
                "Quiz", "Avaliação", ContentType.QUIZ, null, null, false, List.of(), questions);

        draft.applyTo(content);

        assertEquals(ContentType.QUIZ, content.getType());
        assertEquals(questions, content.getQuizQuestions());
    }

    @Test
    void shouldApplyDraftFieldsAndDefaultMissingFreeFlagToFalse() {
        final Content content = content("Aula publicada", "Descrição antiga", true);
        final ContentResource resource = new ContentResource(
                UUID.randomUUID().toString(), ContentResource.TYPE_FILE, "Material", "https://example.com/file.pdf");
        final ContentPendingDraft draft = new ContentPendingDraft(
                "Aula revisada", "Descrição nova", ContentType.FILE,
                "https://example.com/video.mp4", 15, null, List.of(resource), List.of());

        draft.applyTo(content);

        assertEquals("Aula revisada", content.getTitle());
        assertEquals("Descrição nova", content.getDescription());
        assertEquals(ContentType.FILE, content.getType());
        assertEquals("https://example.com/video.mp4", content.getUrl());
        assertEquals(15, content.getDurationMinutes());
        assertFalse(content.getIsFree());
        assertEquals(List.of(resource), content.getResources());
    }

    private static Content content(String title, String description, Boolean isFree) {
        final Content content = new Content();
        content.setTitle(title);
        content.setDescription(description);
        content.setType(ContentType.TEXT);
        content.setIsFree(isFree);
        return content;
    }
}
