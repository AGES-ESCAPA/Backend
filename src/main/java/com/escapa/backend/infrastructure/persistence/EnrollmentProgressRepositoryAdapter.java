package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.escapa.backend.application.port.EnrollmentProgressRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseId;

@Repository
public class EnrollmentProgressRepositoryAdapter implements EnrollmentProgressRepositoryPort {

    private final UserCourseJpaRepository userCourseJpaRepository;

    public EnrollmentProgressRepositoryAdapter(UserCourseJpaRepository userCourseJpaRepository) {
        this.userCourseJpaRepository = userCourseJpaRepository;
    }

    @Override
    @Transactional
    public void updateProgress(UUID userId, UUID courseId, int progress, LocalDate conclusionDate) {
        userCourseJpaRepository.findById(new UserCourseId(userId, courseId))
                .filter(enrollment -> enrollment.getRevokedAt() == null)
                .ifPresent(enrollment -> {
                    enrollment.setProgress(progress);
                    if (conclusionDate != null && enrollment.getConclusionDate() == null) {
                        enrollment.setConclusionDate(conclusionDate);
                    }
                });
    }
}
