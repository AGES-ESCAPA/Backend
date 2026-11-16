package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CurriculumReleasePort;
import com.escapa.backend.domain.content.ContentPendingDraft;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class CurriculumReleaseAdapter implements CurriculumReleasePort {

    private final ModuleJpaRepository moduleJpaRepository;
    private final ContentJpaRepository contentJpaRepository;

    public CurriculumReleaseAdapter(
            ModuleJpaRepository moduleJpaRepository,
            ContentJpaRepository contentJpaRepository
    ) {
        this.moduleJpaRepository = moduleJpaRepository;
        this.contentJpaRepository = contentJpaRepository;
    }

    @Override
    @Transactional
    public List<ReleasedLessonChange> release(UUID courseId) {
        final List<ReleasedLessonChange> changes = new ArrayList<>();
        final List<ModuleEntity> modulesToDelete = new ArrayList<>();
        for (final ModuleEntity module : moduleJpaRepository.findByCourseIdOrderByOrderAsc(courseId)) {
            if (module.isPendingDeletion()) {
                modulesToDelete.add(module);
                continue;
            }
            publishModule(module, changes);
        }
        modulesToDelete.forEach(moduleJpaRepository::delete);
        return changes;
    }

    private void publishModule(ModuleEntity module, List<ReleasedLessonChange> changes) {
        if (module.getPendingTitle() != null) {
            module.setTitle(module.getPendingTitle());
            module.setPendingTitle(null);
        }
        module.setPublishedOrder(module.getOrder());
        final List<ContentEntity> contents = module.getContents() == null
                ? List.of()
                : new ArrayList<>(module.getContents());
        final List<ContentEntity> contentsToDelete = new ArrayList<>();
        for (final ContentEntity content : contents) {
            if (content.isPendingDeletion()) {
                contentsToDelete.add(content);
                continue;
            }
            publishContent(content, changes);
        }
        contentsToDelete.forEach(content -> deleteContent(module, content));
    }

    private void publishContent(ContentEntity content, List<ReleasedLessonChange> changes) {
        final Content published = ContentMapper.toDomain(content);
        final ContentPendingDraft draft = published.getPendingDraft();
        if (draft != null && content.getPublishedOrder() != null && draft.differsFrom(published)) {
            changes.add(new ReleasedLessonChange(content.getId(), draft.title()));
        }
        if (draft != null) {
            draft.applyTo(published);
            published.setPendingDraft(null);
            ContentMapper.applyToEntity(published, content, content.getModule());
        }
        content.setPendingDraft(null);
        content.setPublishedOrder(content.getOrder());
    }

    private void deleteContent(ModuleEntity module, ContentEntity content) {
        if (module.getContents() != null) {
            module.getContents().remove(content);
        }
        contentJpaRepository.delete(content);
    }
}
