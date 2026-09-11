package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.port.CourseRepositoryPort;

/**
 * Caso de uso: listar cursos publicados com filtros e paginação.
 * Normaliza os filtros (trim, vazio vira ausente), valida os parâmetros
 * de paginação e delega ao repositório.
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
        return courseRepositoryPort.findPublished(
                normalizeFilter(title), normalizeFilter(category), normalizeFilter(level),
                safePage, safeSize);
    }

    /**
     * Remove espaços nas pontas e trata string vazia como filtro ausente,
     * para que {@code ?category=} ou {@code ?title=%20} não alterem o resultado.
     */
    private static String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
