package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Module;

import java.util.List;
import java.util.UUID;

public class ListCourseModulesUseCase {

    private final ModuleRepositoryPort moduleRepositoryPort;
    private final CourseRepositoryPort courseRepositoryPort;

    public ListCourseModulesUseCase(
            ModuleRepositoryPort moduleRepositoryPort,
            CourseRepositoryPort courseRepositoryPort
    ) {
        this.moduleRepositoryPort = moduleRepositoryPort;
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public List<Module> execute(UUID courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("courseId is required");
        }
        if (!courseRepositoryPort.existsById(courseId)) {
            throw new CourseNotFoundException(courseId);
        }
        return moduleRepositoryPort.findByCourseId(courseId);
    }
}
