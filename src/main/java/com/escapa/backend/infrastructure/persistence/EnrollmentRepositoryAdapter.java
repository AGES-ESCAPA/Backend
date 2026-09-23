package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.domain.course.EnrollmentStatus;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class EnrollmentRepositoryAdapter implements EnrollmentRepositoryPort {

    private final UserCourseJpaRepository userCourseJpaRepository;

    public EnrollmentRepositoryAdapter(UserCourseJpaRepository userCourseJpaRepository) {
        this.userCourseJpaRepository = userCourseJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId) {
        return userCourseJpaRepository.findPeriodByUserIdAndCourseId(userId, courseId)
                .map(period -> new Enrollment(period.getDtInicio(), period.getDtExpiracao()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StudentCourseCard> findStudentEnrollments(UUID userId, String title, EnrollmentStatus status, int page, int size) {
        final Pageable pageable = PageRequest.of(page, size);
        final String titlePattern = (title == null || title.isBlank()) ? "%%" : "%" + title.toLowerCase() + "%";
        final String statusStr = status != null ? status.name() : null;

        final Page<UserCourseEntity> entityPage = userCourseJpaRepository.findStudentEnrollments(userId, titlePattern, statusStr, pageable);

        final List<StudentCourseCard> content = entityPage.getContent().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        return new PageResult<>(
                content,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    private StudentCourseCard toDomain(UserCourseEntity entity) {
        return new StudentCourseCard(
                entity.getCourse().getId(),
                entity.getCourse().getTitle(),
                entity.getCourse().getInstructor() != null ? entity.getCourse().getInstructor().getName() : null,
                entity.getCourse().getThumbnailUrl(),
                entity.getCourse().getDurationTime(),
                entity.getCourse().getLessonsCount(),
                entity.getProgress(),
                calculateStatus(entity)
        );
    }

    private EnrollmentStatus calculateStatus(UserCourseEntity entity) {
        if (entity.getConclusionDate() != null || (entity.getProgress() != null && entity.getProgress() == 100)) {
            return EnrollmentStatus.COMPLETED;
        }

        if (entity.getDtExpiracao() != null && entity.getDtExpiracao().isBefore(LocalDate.now())) {
            return EnrollmentStatus.EXPIRED;
        }

        if (entity.getDtInicio() != null && entity.getDtInicio().isAfter(LocalDate.now())) {
            return EnrollmentStatus.PENDING;
        }

        return EnrollmentStatus.IN_PROGRESS;
    }
}
