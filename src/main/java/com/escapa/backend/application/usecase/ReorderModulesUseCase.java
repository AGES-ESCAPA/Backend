package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Module;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ReorderModulesUseCase {

    private final ModuleRepositoryPort moduleRepositoryPort;
    private final CourseRepositoryPort courseRepositoryPort;

    public ReorderModulesUseCase(
            ModuleRepositoryPort moduleRepositoryPort,
            CourseRepositoryPort courseRepositoryPort
    ) {
        this.moduleRepositoryPort = moduleRepositoryPort;
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public List<Module> execute(UUID courseId, List<UUID> orderedIds) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId is required");
        }
        if (!courseRepositoryPort.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("moduleIds is required");
        }

        final Set<UUID> requested = new HashSet<>(orderedIds);
        if (requested.size() != orderedIds.size()) {
            throw new IllegalArgumentException("moduleIds must not contain duplicates");
        }

        final Set<UUID> current = new HashSet<>(
                moduleRepositoryPort.findByCourseId(courseId).stream().map(Module::getId).toList());
        if (!current.equals(requested)) {
            // Reordenacao parcial deixaria buracos ou colisoes na sequencia do curso.
            throw new IllegalArgumentException("moduleIds must contain every module of the course exactly once");
        }

        moduleRepositoryPort.reorder(courseId, orderedIds);
        return moduleRepositoryPort.findByCourseId(courseId);
    }
}
