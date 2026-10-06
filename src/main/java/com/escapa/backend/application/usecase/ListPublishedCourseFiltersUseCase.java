package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PublishedCourseFilters;
import com.escapa.backend.application.port.CourseRepositoryPort;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Caso de uso: listar as categorias e os níveis disponíveis na vitrine pública.
 */
public class ListPublishedCourseFiltersUseCase {

    private static final List<String> LEVEL_ORDER = List.of("INICIANTE", "INTERMEDIARIO", "AVANCADO");

    private final CourseRepositoryPort courseRepositoryPort;

    public ListPublishedCourseFiltersUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public PublishedCourseFilters execute() {
        final PublishedCourseFilters filters = courseRepositoryPort.findPublishedFilters();
        final List<String> levels = filters.levels().stream()
                .sorted(Comparator.comparingInt(this::levelRank).thenComparing(String::compareTo))
                .toList();
        return new PublishedCourseFilters(filters.categories(), levels);
    }

    private int levelRank(String level) {
        final String key = Normalizer.normalize(level, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT);
        final int index = LEVEL_ORDER.indexOf(key);
        return index < 0 ? LEVEL_ORDER.size() : index;
    }
}
