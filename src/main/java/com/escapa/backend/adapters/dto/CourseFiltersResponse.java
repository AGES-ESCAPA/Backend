package com.escapa.backend.adapters.dto;

import java.util.List;

import com.escapa.backend.application.dto.PublishedCourseFilters;

/** Opções de filtro da vitrine pública. */
public record CourseFiltersResponse(List<String> categories, List<String> levels) {


    public static CourseFiltersResponse from(PublishedCourseFilters filters) {
        return new CourseFiltersResponse(filters.categories(), filters.levels());
    }
}
