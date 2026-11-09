package com.escapa.backend.application.port;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CourseEnrollmentCount;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.domain.course.EnrollmentStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepositoryPort {

    Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId);

    PageResult<StudentCourseCard> findStudentEnrollments(UUID userId, String title, EnrollmentStatus status, int page, int size);

    /**
     * Registra o instante do ultimo acesso do aluno ao curso (US-35). So grava se ainda nao houver acesso
     * registrado ou se o ultimo for anterior a {@code minInterval} antes de {@code accessedAt}, para
     * recargas seguidas nao virarem uma escrita cada. Sem matricula nao revogada (inexistente ou
     * revogada) nao faz nada e nunca cria matricula; vencida ou ainda nao iniciada registra normalmente.
     */
    void recordLastAccess(UUID userId, UUID courseId, LocalDateTime accessedAt, Duration minInterval);

    List<CourseEnrollmentCount> countEligibleEnrollmentsByCourse();
}
