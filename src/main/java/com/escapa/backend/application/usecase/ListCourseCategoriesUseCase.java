package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;

import java.text.Collator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lista as categorias já cadastradas em cursos, sem duplicar variações que
 * diferem só por maiúsculas e minúsculas.
 */
public class ListCourseCategoriesUseCase {

    private final CourseRepositoryPort courseRepositoryPort;

    public ListCourseCategoriesUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public List<String> execute() {
        final Map<String, String> unique = new LinkedHashMap<>();
        for (String category : courseRepositoryPort.findDistinctCategories()) {
            if (category == null) {
                continue;
            }
            final String trimmed = category.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            unique.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
        }

        final Collator collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        return unique.values().stream().sorted(collator::compare).toList();
    }
}
