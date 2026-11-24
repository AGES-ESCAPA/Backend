package com.escapa.backend.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.content.ContentPendingDraft;
import com.escapa.backend.domain.content.ContentResource;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.QuizQuestion;

class ContentTest {

    @Test
    void shouldCopyDefensivelyIncludingNullCollections() {
        final Content original = new Content();
        original.setId(UUID.randomUUID());
        original.setModuleId(UUID.randomUUID());
        original.setTitle("Aula");
        original.setDescription("Descricao");
        original.setType(ContentType.TEXT);
        original.setUrl(null);
        original.setDurationMinutes(null);
        original.setIsFree(true);
        original.setOrder(1);
        original.setResources(null);
        original.setQuizQuestions(null);
        original.setPublishedOrder(2);
        original.setPendingDraft(new ContentPendingDraft(
                "Rascunho", null, ContentType.TEXT, null, null, true, List.of(), List.of()));
        original.setPendingDeletion(false);
        original.setCoursePublished(true);

        final Content copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getId(), copy.getId());
        assertEquals("Aula", copy.getTitle());
        assertTrue(copy.getResources().isEmpty());
        assertTrue(copy.getQuizQuestions().isEmpty());
        assertEquals(original.getPendingDraft(), copy.getPendingDraft());
        assertTrue(copy.isHeldForPublish());
    }

    @Test
    void shouldCopyCollectionsWithoutSharingMutableState() {
        final Content original = new Content();
        original.setResources(new ArrayList<>(List.of(
                new ContentResource("link", "link", "Fonte", "https://example.com"))));
        original.setQuizQuestions(new ArrayList<>(List.of(
                new QuizQuestion(UUID.randomUUID(), "Pergunta?", List.of()))));

        final Content copy = original.copy();
        original.getResources().clear();
        original.getQuizQuestions().clear();

        assertEquals(1, copy.getResources().size());
        assertEquals(1, copy.getQuizQuestions().size());
    }

    @Test
    void shouldNotHoldForPublishWithoutPublishedOrder() {
        final Content content = new Content();
        content.setCoursePublished(true);
        content.setPublishedOrder(null);

        assertFalse(content.isHeldForPublish());
    }
}
