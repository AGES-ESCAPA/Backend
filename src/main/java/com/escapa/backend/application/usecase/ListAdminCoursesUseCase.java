package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;

import java.util.Comparator;
import java.util.List;

/**
 * Lista os cursos do painel administrativo: rascunhos e publicados,
 * sem os arquivados (o DELETE administrativo é um soft-delete).
 */
public class ListAdminCoursesUseCase {

    private final CourseRepositoryPort courseRepositoryPort;

    public ListAdminCoursesUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public List<Course> execute() {
        return courseRepositoryPort.findAll().stream()
                .filter(course -> course.getStatus() != CourseStatus.ARCHIVED)
                .sorted(Comparator.comparing(
                        Course::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }
}
