package com.escapa.backend.adapters.dto;

import java.util.UUID;

import com.escapa.backend.domain.entity.Course;

public record CourseSearchResponse(UUID id, String title) {


    public static CourseSearchResponse from(Course course) {
        return new CourseSearchResponse(course.getId(), course.getTitle());
    }
}
