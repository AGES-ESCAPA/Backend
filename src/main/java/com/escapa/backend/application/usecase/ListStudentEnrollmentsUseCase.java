package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.domain.course.EnrollmentStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ListStudentEnrollmentsUseCase {

    private final EnrollmentRepositoryPort enrollmentRepositoryPort;

    public ListStudentEnrollmentsUseCase(EnrollmentRepositoryPort enrollmentRepositoryPort) {
        this.enrollmentRepositoryPort = enrollmentRepositoryPort;
    }

    public PageResult<StudentCourseCard> execute(UUID userId, String title, EnrollmentStatus status, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
        
        return enrollmentRepositoryPort.findStudentEnrollments(userId, title, status, page, size);
    }
}

