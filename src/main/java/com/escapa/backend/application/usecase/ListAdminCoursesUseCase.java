package com.escapa.backend.application.usecase;

import java.util.Comparator;
import java.util.List;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.entity.Course;

/**
 * Lista os cursos do painel administrativo, incluindo arquivados
 * (o DELETE administrativo é um soft-delete e continua filtrável no painel).
 */
public class ListAdminCoursesUseCase {

    private final CourseRepositoryPort courseRepositoryPort;

    public ListAdminCoursesUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public List<Course> execute() {
        return courseRepositoryPort.findAllForListing().stream()
                .sorted(Comparator.comparing(
                        Course::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }
}
