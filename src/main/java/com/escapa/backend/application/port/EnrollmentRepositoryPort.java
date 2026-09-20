package com.escapa.backend.application.port;

import com.escapa.backend.application.model.Enrollment;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepositoryPort {

    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);
}
