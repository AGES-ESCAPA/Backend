package com.escapa.backend.application.port;

import java.util.List;
import java.util.UUID;

/**
 * Relação N:N autorreferenciada de pré-requisitos entre cursos (US-09). Trabalha
 * só com ids: a resolução para {@code CourseEntity} (JPA) fica inteiramente na
 * implementação de infraestrutura.
 */
public interface CoursePrerequisiteRepositoryPort {

    List<UUID> findPrerequisiteCourseIds(UUID courseId);

    boolean existsByCourseIdAndPrerequisiteCourseId(UUID courseId, UUID prerequisiteCourseId);

    void save(UUID courseId, UUID prerequisiteCourseId);

    void deleteByCourseIdAndPrerequisiteCourseId(UUID courseId, UUID prerequisiteCourseId);
}
