package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;

import java.util.ArrayList;

public final class ModuleMapper {

    private ModuleMapper() {
    }

    /** Converte sem materializar o agregado Course: so o id do curso e propagado. */
    public static Module toDomain(ModuleEntity entity) {
        if (entity == null) {
            return null;
        }
        final boolean coursePublished = entity.getCourse() != null
                && entity.getCourse().getStatus() == CourseStatus.PUBLISHED;
        final Module module = new Module(
                entity.getId(),
                entity.getCourse() != null ? entity.getCourse().getId() : null,
                entity.getTitle(),
                entity.getOrder()
        );
        module.setPublishedOrder(entity.getPublishedOrder());
        module.setPendingTitle(entity.getPendingTitle());
        module.setPendingDeletion(entity.isPendingDeletion());
        module.setCoursePublished(coursePublished);
        if (entity.getContents() != null) {
            module.setContents(new ArrayList<>(entity.getContents().stream()
                    .map(ContentMapper::toDomain)
                    .toList()));
            module.getContents().forEach(content -> content.setCoursePublished(coursePublished));
        }
        return module;
    }

    /** Copia os campos editaveis. Conteudos e pre-requisitos sao gerenciados por outros fluxos. */
    public static void applyToEntity(Module module, ModuleEntity entity, CourseEntity course) {
        entity.setCourse(course);
        entity.setTitle(module.getTitle());
        entity.setOrder(module.getOrder());
        entity.setPublishedOrder(module.getPublishedOrder());
        entity.setPendingTitle(module.getPendingTitle());
        entity.setPendingDeletion(module.isPendingDeletion());
    }
}
