package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

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
}
