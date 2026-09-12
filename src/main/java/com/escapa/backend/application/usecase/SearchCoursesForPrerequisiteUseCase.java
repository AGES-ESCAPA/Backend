package com.escapa.backend.application.usecase;

import java.util.List;
import java.util.UUID;

import com.escapa.backend.adapters.dto.CourseSearchResponse;
import com.escapa.backend.application.port.CourseRepositoryPort;

public class SearchCoursesForPrerequisiteUseCase {
    private final CourseRepositoryPort courseRepository;

    public SearchCoursesForPrerequisiteUseCase(CourseRepositoryPort courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseSearchResponse> execute(UUID courseId, String query) {
        final String normalizedQuery = query == null ? "" : query.trim();
        return courseRepository.searchByTitle(normalizedQuery, courseId).stream()
                .map(course -> new CourseSearchResponse(course.getId(), course.getTitle()))
                .toList();
    }
}
