package com.escapa.backend.application.port;

import com.escapa.backend.domain.entity.Course;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepositoryPort {
    Course save(Course course);
    Optional<Course> findById(UUID id);
    List<Course> findAll();
}
