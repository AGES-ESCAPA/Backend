package com.escapa.backend.application.dto;

import java.util.List;

/** Categorias e níveis distintos dos cursos publicados. */
public record PublishedCourseFilters(List<String> categories, List<String> levels) {
}
