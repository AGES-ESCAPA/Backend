package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;

public interface UserCourseJpaRepository
        extends JpaRepository<UserCourseEntity, com.escapa.backend.infrastructure.persistence.entity.UserCourseId> {
            @Query("select enrollment from UserCourseEntity enrollment "
                + "where enrollment.id.courseId = :courseId "
                + "and (enrollment.dtExpiracao is null or enrollment.dtExpiracao >= :today)")
            List<UserCourseEntity> findActiveByCourseId(
                @Param("courseId") UUID courseId, @Param("today") LocalDate today);

            /** Projecao so com o periodo, para nao carregar usuario e curso da matricula. */
            interface EnrollmentPeriod {
                LocalDate getDtInicio();

                LocalDate getDtExpiracao();
            }

            @Query("select enrollment.dtInicio as dtInicio, enrollment.dtExpiracao as dtExpiracao "
                + "from UserCourseEntity enrollment "
                + "where enrollment.id.userId = :userId and enrollment.id.courseId = :courseId")
            Optional<EnrollmentPeriod> findPeriodByUserIdAndCourseId(
                @Param("userId") UUID userId, @Param("courseId") UUID courseId);

            @Query(value = """
                SELECT uc FROM UserCourseEntity uc
                JOIN FETCH uc.course c
                LEFT JOIN FETCH c.instructor
                WHERE uc.user.id = :userId
                AND (:titlePattern = '%%' OR LOWER(c.title) LIKE :titlePattern)
                AND (
                    :status IS NULL
                    OR (:status = 'COMPLETED' AND (uc.conclusionDate IS NOT NULL OR uc.progress = 100))
                    OR (:status = 'EXPIRED' AND (uc.conclusionDate IS NULL AND (uc.progress IS NULL OR uc.progress < 100)) AND uc.dtExpiracao < CURRENT_DATE)
                    OR (:status = 'PENDING' AND uc.dtInicio > CURRENT_DATE)
                    OR (:status = 'IN_PROGRESS' AND (uc.conclusionDate IS NULL AND (uc.progress IS NULL OR uc.progress < 100)) AND (uc.dtExpiracao IS NULL OR uc.dtExpiracao >= CURRENT_DATE) AND (uc.dtInicio IS NULL OR uc.dtInicio <= CURRENT_DATE))
                )
                """,
                countQuery = """
                SELECT COUNT(uc) FROM UserCourseEntity uc
                JOIN uc.course c
                WHERE uc.user.id = :userId
                AND (:titlePattern = '%%' OR LOWER(c.title) LIKE :titlePattern)
                AND (
                    :status IS NULL
                    OR (:status = 'COMPLETED' AND (uc.conclusionDate IS NOT NULL OR uc.progress = 100))
                    OR (:status = 'EXPIRED' AND (uc.conclusionDate IS NULL AND (uc.progress IS NULL OR uc.progress < 100)) AND uc.dtExpiracao < CURRENT_DATE)
                    OR (:status = 'PENDING' AND uc.dtInicio > CURRENT_DATE)
                    OR (:status = 'IN_PROGRESS' AND (uc.conclusionDate IS NULL AND (uc.progress IS NULL OR uc.progress < 100)) AND (uc.dtExpiracao IS NULL OR uc.dtExpiracao >= CURRENT_DATE) AND (uc.dtInicio IS NULL OR uc.dtInicio <= CURRENT_DATE))
                )
                """)
            Page<UserCourseEntity> findStudentEnrollments(
                @Param("userId") UUID userId,
                @Param("titlePattern") String titlePattern,
                @Param("status") String status,
                Pageable pageable
            );
}
