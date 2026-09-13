package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Course;

import java.util.UUID;

/**
 * Busca um curso pelo id para o painel administrativo (edição no Construtor
 * de Curso), independente do status (DRAFT/PUBLISHED/ARCHIVED).
 */
public class GetAdminCourseUseCase {
    private final CourseRepositoryPort courseRepositoryPort;

    public GetAdminCourseUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public Course execute(UUID id) {
        return courseRepositoryPort.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
    }
}
