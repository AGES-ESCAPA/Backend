package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

            // Aluno e curso vem na mesma consulta (US-19): sao os unicos dados do
            // agregado que o certificado precisa, alem dos campos da propria matricula.
            @Query("select uc from UserCourseEntity uc "
                + "join fetch uc.user join fetch uc.course "
                + "where uc.certificateCode = :code and uc.certificateIssued = true")
            Optional<UserCourseEntity> findIssuedByCertificateCode(@Param("code") String code);

            @Modifying
            @Query("update UserCourseEntity uc set uc.certificatePdf = :pdf "
                + "where uc.id.userId = :userId and uc.id.courseId = :courseId")
            void updateCertificatePdf(
                @Param("userId") UUID userId, @Param("courseId") UUID courseId, @Param("pdf") byte[] pdf);
}
