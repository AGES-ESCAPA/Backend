package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
}
