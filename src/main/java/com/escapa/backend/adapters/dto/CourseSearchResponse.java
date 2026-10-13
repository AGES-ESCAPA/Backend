package com.escapa.backend.adapters.dto;

import com.escapa.backend.domain.entity.Course;
import java.util.UUID;

public record CourseSearchResponse(UUID id, String title) {


    public static CourseSearchResponse from(Course course) {
        return new CourseSearchResponse(course.getId(), course.getTitle());
    }
}
