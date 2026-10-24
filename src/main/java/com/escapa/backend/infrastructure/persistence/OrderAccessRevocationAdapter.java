package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.OrderAccessRevocationPort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public class OrderAccessRevocationAdapter implements OrderAccessRevocationPort {

    private final UserCourseJpaRepository userCourseJpaRepository;
    private final CompanyCourseJpaRepository companyCourseJpaRepository;

    public OrderAccessRevocationAdapter(
            UserCourseJpaRepository userCourseJpaRepository,
            CompanyCourseJpaRepository companyCourseJpaRepository
    ) {
        this.userCourseJpaRepository = userCourseJpaRepository;
        this.companyCourseJpaRepository = companyCourseJpaRepository;
    }

    @Override
    @Transactional
    public void revokeStudentCourseAccess(UUID userId, UUID courseId, LocalDateTime revokedAt) {
        userCourseJpaRepository.revokeByUserIdAndCourseId(userId, courseId, revokedAt);
    }

    @Override
    @Transactional
    public void revokeCompanyCourseAccess(UUID companyId, UUID courseId, LocalDateTime revokedAt) {
        companyCourseJpaRepository.revokeByCompanyIdAndCourseId(companyId, courseId, revokedAt);
        userCourseJpaRepository.revokeByCompanyIdAndCourseId(companyId, courseId, revokedAt);
    }
}
