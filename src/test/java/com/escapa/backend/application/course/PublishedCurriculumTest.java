package com.escapa.backend.application.course;

import com.escapa.backend.domain.content.ContentPendingDraft;
import com.escapa.backend.domain.content.ContentResource;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishedCurriculumTest {

    @Test
    void shouldBuildEditorViewWithPendingChangesAndWithoutDeletedItems() {
        final Content editedContent = content("Publicado", 2, 1);
        final ContentPendingDraft draft = new ContentPendingDraft(
                "Rascunho", "Descrição atualizada", ContentType.TEXT, null, 12, true,
                List.of(new ContentResource("material", "link", "Leitura", "https://example.com")));
        editedContent.setPendingDraft(draft);
        final Content newContent = content("Novo conteúdo", 1, null);
        final Content deletedContent = content("Removido", 3, 2);
        deletedContent.setPendingDeletion(true);

        final Module laterModule = module("Título publicado", 2, 1);
        laterModule.setPendingTitle("Título pendente");
        laterModule.setCoursePublished(true);
        laterModule.setContents(List.of(editedContent, newContent, deletedContent));
        final Module earlierModule = module("Primeiro", 1, 1);
        final Module deletedModule = module("Módulo removido", 3, 2);
        deletedModule.setPendingDeletion(true);

        final List<Module> editorView = PublishedCurriculum.forEditor(
                List.of(laterModule, deletedModule, earlierModule));

        assertEquals(List.of("Primeiro", "Título pendente"),
                editorView.stream().map(Module::getTitle).toList());
        assertEquals(List.of("Novo conteúdo", "Rascunho"),
                editorView.get(1).getContents().stream().map(Content::getTitle).toList());
        assertEquals("Descrição atualizada", editorView.get(1).getContents().get(1).getDescription());
        assertEquals(draft, editorView.get(1).getContents().get(1).getPendingDraft());
        assertTrue(editorView.get(1).isCoursePublished());
        assertEquals("Publicado", editedContent.getTitle());
    }

    @Test
    void shouldHandleEmptyEditorInputsAndBlankPendingTitles() {
        final Module module = module("Published title", 1, null);
        module.setPendingTitle("  ");
        module.setContents(null);

        assertEquals(List.of(), PublishedCurriculum.forEditor(null));
        assertEquals(List.of(), PublishedCurriculum.forEditor(List.of()));
        final Module editorModule = PublishedCurriculum.editorModule(module);
        assertEquals("Published title", editorModule.getTitle());
        assertEquals(List.of(), editorModule.getContents());
    }

    @Test
    void shouldExposeOnlyThePublishedStudentCurriculumInPublishedOrder() {
        final Content visibleContent = content("Updated title", 9, 2);
        visibleContent.setPendingDraft(new ContentPendingDraft(
                "Unpublished title", null, ContentType.TEXT, null, null, null, null));
        final Content unpublishedContent = content("Not published", 1, null);
        final Module laterModule = module("Later", 2, 2);
        laterModule.setContents(List.of(unpublishedContent, visibleContent));
        final Module earlierModule = module("Earlier", 1, 1);
        earlierModule.setContents(null);
        final Module draftModule = module("Draft module", 3, null);
        final List<Module> modules = List.of(laterModule, draftModule, earlierModule);

        final List<Module> studentView = PublishedCurriculum.forStudents(modules, CourseStatus.PUBLISHED);

        assertEquals(List.of("Earlier", "Later"), studentView.stream().map(Module::getTitle).toList());
        assertEquals(List.of("Updated title"), studentView.get(1).getContents().stream()
                .map(Content::getTitle).toList());
        assertEquals(2, studentView.get(1).getContents().get(0).getOrder());
        assertNull(studentView.get(1).getContents().get(0).getPendingDraft());
        assertTrue(studentView.get(1).isCoursePublished());
        assertEquals(List.of(), studentView.get(0).getContents());
    }

    @Test
    void shouldReturnUnpublishedCurriculumUnchangedAndParkOrdersBelowZero() {
        final List<Module> modules = List.of(module("Draft", 4, null));

        assertSame(modules, PublishedCurriculum.forStudents(modules, CourseStatus.DRAFT));
        assertEquals(List.of(), PublishedCurriculum.forStudents(null, CourseStatus.PUBLISHED));
        assertEquals(-1_000_000, PublishedCurriculum.parkedOrder(null));
        assertEquals(-1_000_004, PublishedCurriculum.parkedOrder(4));
    }

    private static Module module(String title, int order, Integer publishedOrder) {
        final Module module = new Module(UUID.randomUUID(), UUID.randomUUID(), title, order);
        module.setPublishedOrder(publishedOrder);
        return module;
    }

    private static Content content(String title, int order, Integer publishedOrder) {
        final Content content = new Content();
        content.setId(UUID.randomUUID());
        content.setTitle(title);
        content.setType(ContentType.TEXT);
        content.setOrder(order);
        content.setPublishedOrder(publishedOrder);
        return content;
    }
}
