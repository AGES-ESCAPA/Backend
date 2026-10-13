package com.escapa.backend.adapters.dto.course;

import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.course.CourseStatus;

import java.util.UUID;

public record AdminCourseListItemResponse(
        UUID id,
        String title,
        String category,
        Double price,
        CourseStatus status,
        Integer majorVersion,
        Integer minorVersion
) {

    public static AdminCourseListItemResponse from(Course course) {
        final int major = course.getMajorVersion() == null ? 0 : course.getMajorVersion();
        final int minor = course.getMinorVersion() == null ? 0 : course.getMinorVersion();
        return new AdminCourseListItemResponse(
                course.getId(),
                course.getTitle(),
                course.getCategory(),
                course.getPrice(),
                course.getStatus(),
                major,
                minor);
    }
}
