package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;

import java.time.LocalDateTime;
import java.util.UUID;

public class ArchiveCourseUseCase {
    private final CourseRepositoryPort courseRepositoryPort;

    public ArchiveCourseUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public Course execute(UUID id) {
        final Course course = courseRepositoryPort.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
        course.setStatus(CourseStatus.ARCHIVED);
        course.setUpdatedAt(LocalDateTime.now());
        return courseRepositoryPort.save(course);
    }
}
