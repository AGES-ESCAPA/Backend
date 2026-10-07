package com.escapa.backend.adapters.dto;

import java.util.List;

/** Opções de filtro da vitrine pública. */
public record CourseFiltersResponse(List<String> categories, List<String> levels) {
}
