package com.escapa.backend.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.escapa.backend.adapters.dto.certificate.CertificateFlatProjectionDTO;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;

public interface CertificateRepository extends JpaRepository<UserCourseEntity, UUID> {

    @Query("""
        SELECT new com.escapa.backend.adapters.dto.certificate.CertificateFlatProjectionDTO(
            enrollment.conclusionDate,
            course.durationTime,
            enrollment.certificateCode,
            user.name,
            user.avatarUrl,
            user.isVerified,
            cast(course.id as string),
            course.title,
            course.description,
            course.category,
            course.level,
            course.thumbnailUrl,
            course.durationTime,
            course.lessonsCount,
            course.ratingAverage,
            course.reviewsCount,
            instructor.name,
            course.price
        )
        FROM UserCourseEntity enrollment
        JOIN enrollment.user user
        JOIN enrollment.course course
        LEFT JOIN course.instructor instructor
        WHERE enrollment.certificateCode = :code
    """)
    Optional<CertificateFlatProjectionDTO> findCertificateDetailsByVerificationCode(
            @Param("code") String code
    );
}