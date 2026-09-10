package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.port.CourseRepositoryPort;

/**
 * Caso de uso: listar cursos publicados com filtros e paginação.
 * Valida os parâmetros de paginação e delega ao repositório.
 */
public class ListPublishedCoursesUseCase {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final CourseRepositoryPort courseRepositoryPort;

    public ListPublishedCoursesUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public PageResult<CourseSummary> execute(
            String title, String category, String level, Integer page, Integer size) {
        final int safePage = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        final int safeSize = (size != null && size > 0) ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
        return courseRepositoryPort.findPublished(title, category, level, safePage, safeSize);
    }
}
