package com.escapa.backend.application.course;

import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;

import java.util.Comparator;
import java.util.List;

/**
 * Em curso publicado, módulos e aulas têm duas leituras. O construtor vê o
 * rascunho (título pendente, aula nova, exclusão e ordem ainda não publicados).
 * O aluno continua na última versão publicada até {@code PublishCourseUseCase}.
 */
public final class PublishedCurriculum {

    private static final int PARKED_ORDER_BASE = -1_000_000;

    private static final Comparator<Integer> BY_POSITION =
            Comparator.nullsLast(Comparator.naturalOrder());

    private PublishedCurriculum() {
    }

    /** Tira a linha da faixa positiva de {@code order} sem colidir com o reorder. */
    public static int parkedOrder(Integer order) {
        final int current = order == null ? 0 : order;
        return PARKED_ORDER_BASE - Math.abs(current);
    }

    public static List<Module> forEditor(List<Module> modules) {
        if (modules == null || modules.isEmpty()) {
            return List.of();
        }
        return modules.stream()
                .filter(module -> !module.isPendingDeletion())
                .map(PublishedCurriculum::editorModule)
                .sorted(Comparator.comparing(Module::getOrder, BY_POSITION))
                .toList();
    }

    public static Module editorModule(Module source) {
        final Module view = new Module(
                source.getId(), source.getCourseId(), source.getTitle(), source.getOrder());
        if (source.getPendingTitle() != null && !source.getPendingTitle().isBlank()) {
            view.setTitle(source.getPendingTitle());
        }
        view.setPublishedOrder(source.getPublishedOrder());
        view.setPendingTitle(source.getPendingTitle());
        view.setPendingDeletion(source.isPendingDeletion());
        view.setCoursePublished(source.isCoursePublished());
        view.setContents(editorContents(source.getContents()));
        return view;
    }

    public static Content editorContent(Content source) {
        final Content view = source.copy();
        if (source.getPendingDraft() != null) {
            source.getPendingDraft().applyTo(view);
        }
        view.setPendingDraft(source.getPendingDraft());
        return view;
    }

    /**
     * Curso que não está publicado devolve a lista como está: o aluno não chega
     * nela pela vitrine, e os testes dessa grade continuam na ordem do construtor.
     */
    public static List<Module> forStudents(List<Module> modules, CourseStatus status) {
        if (status != CourseStatus.PUBLISHED || modules == null) {
            return modules == null ? List.of() : modules;
        }
        return modules.stream()
                .filter(module -> module.getPublishedOrder() != null)
                .map(PublishedCurriculum::studentModule)
                .sorted(Comparator.comparing(Module::getOrder, BY_POSITION))
                .toList();
    }

    private static Module studentModule(Module source) {
        final Module view = new Module(
                source.getId(), source.getCourseId(), source.getTitle(), source.getPublishedOrder());
        view.setPublishedOrder(source.getPublishedOrder());
        view.setCoursePublished(true);
        final List<Content> contents = source.getContents() == null ? List.of() : source.getContents();
        view.setContents(contents.stream()
                .filter(content -> content.getPublishedOrder() != null)
                .map(PublishedCurriculum::studentContent)
                .sorted(Comparator.comparing(Content::getOrder, BY_POSITION))
                .toList());
        return view;
    }

    private static Content studentContent(Content source) {
        final Content view = source.copy();
        view.setOrder(source.getPublishedOrder());
        view.setPendingDraft(null);
        return view;
    }

    private static List<Content> editorContents(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return List.of();
        }
        return contents.stream()
                .filter(content -> !content.isPendingDeletion())
                .map(PublishedCurriculum::editorContent)
                .sorted(Comparator.comparing(Content::getOrder, BY_POSITION))
                .toList();
    }
}
