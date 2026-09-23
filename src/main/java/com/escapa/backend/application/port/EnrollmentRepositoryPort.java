package com.escapa.backend.application.port;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.domain.course.EnrollmentStatus;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepositoryPort {

    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    PageResult<StudentCourseCard> findStudentEnrollments(UUID userId, String title, EnrollmentStatus status, int page, int size);
}
