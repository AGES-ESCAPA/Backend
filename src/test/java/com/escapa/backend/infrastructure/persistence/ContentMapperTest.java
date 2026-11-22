package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.content.ContentPendingDraft;
import com.escapa.backend.domain.content.ContentResource;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.QuizOption;
import com.escapa.backend.domain.content.QuizQuestion;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;

class ContentMapperTest {

    @Test
    void shouldHandleMissingEntitiesAndInvalidResourceJson() {
        assertNull(ContentMapper.toDomain(null));
        assertNull(ContentMapper.readPendingDraft(null));
        assertNull(ContentMapper.readPendingDraft("  "));

        final ContentEntity entity = new ContentEntity();
        entity.setRecursos("{invalid");

        final Content restored = ContentMapper.toDomain(entity);

        assertNull(restored.getModuleId());
        assertTrue(restored.getResources().isEmpty());
        assertNull(restored.getPendingDraft());
    }

    @Test
    void shouldRoundTripPendingDraftAndResources() {
        final ContentResource resource = new ContentResource(
                "resource-1", ContentResource.TYPE_LINK, "Referência", "https://example.com");
        final ContentPendingDraft draft = new ContentPendingDraft(
                "Título", "Descrição", ContentType.VIDEO, "https://video.example.com",
                15, true, List.of(resource), List.of());
        final ContentEntity entity = new ContentEntity();
        entity.setId(UUID.randomUUID());
        final ModuleEntity module = new ModuleEntity();
        module.setId(UUID.randomUUID());
        entity.setModule(module);
        entity.setRecursos("[{\"id\":\"resource-1\",\"type\":\"link\",\"title\":\"Referência\","
                + "\"url\":\"https://example.com\"}]");
        entity.setPendingDraft(ContentMapper.writePendingDraft(draft));

        final Content restored = ContentMapper.toDomain(entity);

        assertEquals(module.getId(), restored.getModuleId());
        assertEquals(List.of(resource), restored.getResources());
        assertEquals(draft, restored.getPendingDraft());
    }

    @Test
    void shouldRejectInvalidPendingDraftJsonAndReturnNullForMissingDraft() {
        assertNull(ContentMapper.writePendingDraft(null));
        assertThrows(IllegalStateException.class, () -> ContentMapper.readPendingDraft("{invalid"));
    }

    @Test
    void shouldRoundTripQuizQuestionsAndIgnoreInvalidQuizJson() {
        final UUID questionId = UUID.randomUUID();
        final UUID correctId = UUID.randomUUID();
        final UUID wrongId = UUID.randomUUID();
        final List<QuizQuestion> questions = List.of(new QuizQuestion(
                questionId,
                "Pergunta",
                List.of(new QuizOption(correctId, "Correta", true), new QuizOption(wrongId, "Errada", false))));
        final Content content = new Content();
        content.setTitle("Quiz");
        content.setType(ContentType.QUIZ);
        content.setQuizQuestions(questions);
        final ContentEntity entity = new ContentEntity();

        ContentMapper.applyToEntity(content, entity, null);
        assertNotNull(entity.getQuizJson());
        assertTrue(entity.getQuizJson().contains(questionId.toString()));

        final Content restored = ContentMapper.toDomain(entity);
        assertEquals(questions, restored.getQuizQuestions());

        final ContentEntity broken = new ContentEntity();
        broken.setQuizJson("{invalid");
        assertTrue(ContentMapper.toDomain(broken).getQuizQuestions().isEmpty());

        final ContentEntity blankQuiz = new ContentEntity();
        blankQuiz.setQuizJson("  ");
        assertTrue(ContentMapper.toDomain(blankQuiz).getQuizQuestions().isEmpty());

        final Content empty = new Content();
        empty.setQuizQuestions(List.of());
        final ContentEntity emptyEntity = new ContentEntity();
        ContentMapper.applyToEntity(empty, emptyEntity, null);
        assertNull(emptyEntity.getQuizJson());
    }

    @Test
    void shouldMapNullableFreeFlagResourcesAndCreationTime() {
        final ContentEntity entity = new ContentEntity();
        final Content content = new Content();
        ContentMapper.applyToEntity(content, entity, null);

        assertFalse(entity.getIsFree());
        assertNull(entity.getRecursos());
        assertNotNull(entity.getCreatedAt());

        final LocalDateTime createdAt = LocalDateTime.of(2026, 11, 17, 12, 0);
        final Content populated = new Content();
        populated.setIsFree(true);
        populated.setResources(List.of(new ContentResource(
                "resource-2", ContentResource.TYPE_FILE, "Arquivo", "https://example.com/file.pdf")));
        populated.setCreatedAt(createdAt);
        populated.setPendingDraft(new ContentPendingDraft(
                "Título", null, ContentType.VIDEO, null, null, null, List.of(), List.of()));
        final ContentEntity populatedEntity = new ContentEntity();

        ContentMapper.applyToEntity(populated, populatedEntity, null);

        assertTrue(populatedEntity.getIsFree());
        assertTrue(populatedEntity.getRecursos().contains("resource-2"));
        assertEquals(createdAt, populatedEntity.getCreatedAt());
        assertNotNull(populatedEntity.getPendingDraft());
    }
}
