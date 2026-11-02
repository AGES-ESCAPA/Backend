package com.escapa.backend.application.port;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.domain.course.EnrollmentStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepositoryPort {

    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    PageResult<StudentCourseCard> findStudentEnrollments(UUID userId, String title, EnrollmentStatus status, int page, int size);

    /**
     * Registra o instante do ultimo acesso do aluno ao curso (US-35). Sem
     * matricula ativa (inexistente ou revogada) nao faz nada e nunca cria matricula.
     */
    void recordLastAccess(UUID userId, UUID courseId, LocalDateTime accessedAt);
}
