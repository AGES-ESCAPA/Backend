package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Module;

import java.util.UUID;

/**
 * Cria o modulo sempre no fim da lista do curso: {@code order} nao vem do cliente,
 * e calculado como max(order) + 1 para respeitar uk_modules_course_order.
 */
public class CreateModuleUseCase {

    private final ModuleRepositoryPort moduleRepositoryPort;
    private final CourseRepositoryPort courseRepositoryPort;

    public CreateModuleUseCase(
            ModuleRepositoryPort moduleRepositoryPort,
            CourseRepositoryPort courseRepositoryPort
    ) {
        this.moduleRepositoryPort = moduleRepositoryPort;
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public Module execute(UUID courseId, String title) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId is required");
        }
        if (!courseRepositoryPort.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }

        final Module module = new Module();
        module.setCourseId(courseId);
        module.setTitle(title.trim());
        module.setOrder(moduleRepositoryPort.nextOrder(courseId));
        return moduleRepositoryPort.save(module);
    }
}
