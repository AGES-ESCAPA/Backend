package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.entity.Course;

import java.util.List;
import java.util.UUID;

public class SearchCoursesForPrerequisiteUseCase {

    private final CourseRepositoryPort courseRepository;

    public SearchCoursesForPrerequisiteUseCase(CourseRepositoryPort courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> execute(UUID courseId, String query) {
        final String normalizedQuery = query == null ? "" : query.trim();
        return courseRepository.searchByTitle(normalizedQuery, courseId);
    }
}
