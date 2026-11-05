package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;

public interface UserCourseJpaRepository
        extends JpaRepository<UserCourseEntity, com.escapa.backend.infrastructure.persistence.entity.UserCourseId> {
            @Query("select enrollment from UserCourseEntity enrollment "
                + "where enrollment.id.courseId = :courseId and enrollment.revokedAt is null "
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
                + "where enrollment.id.userId = :userId and enrollment.id.courseId = :courseId "
                + "and enrollment.revokedAt is null")
            Optional<EnrollmentPeriod> findPeriodByUserIdAndCourseId(
                @Param("userId") UUID userId, @Param("courseId") UUID courseId);

            @Query(value = """
                SELECT uc FROM UserCourseEntity uc
                JOIN FETCH uc.course c
                LEFT JOIN FETCH c.instructor
                WHERE uc.user.id = :userId AND uc.revokedAt IS NULL
                AND (:titlePattern = '%%' OR LOWER(c.title) LIKE :titlePattern)
                AND (
                    :status IS NULL
                    OR (:status = 'COMPLETED' AND (uc.conclusionDate IS NOT NULL OR uc.progress = 100))
                    OR (:status = 'EXPIRED' AND uc.conclusionDate IS NULL AND (uc.progress IS NULL OR uc.progress < 100) AND uc.dtExpiracao IS NOT NULL AND uc.dtExpiracao < CURRENT_DATE)
                    OR (:status = 'PENDING' AND uc.conclusionDate IS NULL AND (uc.progress IS NULL OR uc.progress < 100) AND (uc.dtExpiracao IS NULL OR uc.dtExpiracao >= CURRENT_DATE) AND uc.dtInicio > CURRENT_DATE)
                    OR (:status = 'IN_PROGRESS' AND uc.conclusionDate IS NULL AND (uc.progress IS NULL OR uc.progress < 100) AND (uc.dtExpiracao IS NULL OR uc.dtExpiracao >= CURRENT_DATE) AND (uc.dtInicio IS NULL OR uc.dtInicio <= CURRENT_DATE))
                )
                ORDER BY uc.dtInicio DESC, c.id
                """,
                countQuery = """
                SELECT COUNT(uc) FROM UserCourseEntity uc
                JOIN uc.course c
                WHERE uc.user.id = :userId AND uc.revokedAt IS NULL
                AND (:titlePattern = '%%' OR LOWER(c.title) LIKE :titlePattern)
                AND (
                    :status IS NULL
                    OR (:status = 'COMPLETED' AND (uc.conclusionDate IS NOT NULL OR uc.progress = 100))
                    OR (:status = 'EXPIRED' AND uc.conclusionDate IS NULL AND (uc.progress IS NULL OR uc.progress < 100) AND uc.dtExpiracao IS NOT NULL AND uc.dtExpiracao < CURRENT_DATE)
                    OR (:status = 'PENDING' AND uc.conclusionDate IS NULL AND (uc.progress IS NULL OR uc.progress < 100) AND (uc.dtExpiracao IS NULL OR uc.dtExpiracao >= CURRENT_DATE) AND uc.dtInicio > CURRENT_DATE)
                    OR (:status = 'IN_PROGRESS' AND uc.conclusionDate IS NULL AND (uc.progress IS NULL OR uc.progress < 100) AND (uc.dtExpiracao IS NULL OR uc.dtExpiracao >= CURRENT_DATE) AND (uc.dtInicio IS NULL OR uc.dtInicio <= CURRENT_DATE))
                )
                """)
            Page<UserCourseEntity> findStudentEnrollments(
                @Param("userId") UUID userId,
                @Param("titlePattern") String titlePattern,
                @Param("status") String status,
                Pageable pageable
            );

            /** Revoga a matricula do aluno no curso, sem apagar progresso nem certificado. */
            @Modifying
            @Query("update UserCourseEntity uc set uc.revokedAt = :revokedAt "
                + "where uc.id.userId = :userId and uc.id.courseId = :courseId and uc.revokedAt is null")
            int revokeByUserIdAndCourseId(
                @Param("userId") UUID userId,
                @Param("courseId") UUID courseId,
                @Param("revokedAt") LocalDateTime revokedAt);

            /** Revoga as matriculas dos colaboradores da empresa no curso. */
            @Modifying
            @Query("update UserCourseEntity uc set uc.revokedAt = :revokedAt "
                + "where uc.id.courseId = :courseId and uc.revokedAt is null "
                + "and uc.id.userId in (select member.id.userId from UsersCompanyEntity member "
                + "where member.id.companyId = :companyId)")
            int revokeByCompanyIdAndCourseId(
                @Param("companyId") UUID companyId,
                @Param("courseId") UUID courseId,
                @Param("revokedAt") LocalDateTime revokedAt);

            /** Projecao plana da tela de certificado (US-18); o adapter monta o modelo. */
            interface CertificateDetailsView {
                UUID getStudentId();

                LocalDate getConclusionDate();

                String getVerificationCode();

                String getStudentName();

                String getStudentAvatarUrl();

                boolean isStudentVerified();

                UUID getCourseId();

                String getCourseTitle();

                String getCourseDescription();

                String getCourseCategory();

                String getCourseLevel();

                String getCourseThumbnailUrl();

                Integer getCourseDurationTime();

                Integer getCourseLessonsCount();

                Double getCoursePrice();

                String getInstructorName();

                Double getCourseRating();

                Long getCourseReviewsCount();
            }

            // Uma unica consulta para a tela inteira (US-18). avatar_url so existe em
            // admins, dai o LEFT JOIN pelo id do aluno; rating e reviewsCount sao
            // agregados de course_reviews (indice idx_course_reviews_course).
            @Query("select student.id as studentId, uc.conclusionDate as conclusionDate, uc.certificateCode as verificationCode, "
                + "student.name as studentName, studentAdmin.avatarUrl as studentAvatarUrl, "
                + "case when student.status = "
                + "com.escapa.backend.infrastructure.persistence.entity.enums.UserStatus.ACTIVE "
                + "then true else false end as studentVerified, "
                + "course.id as courseId, course.title as courseTitle, "
                + "course.description as courseDescription, course.category as courseCategory, "
                + "course.level as courseLevel, course.thumbnailUrl as courseThumbnailUrl, "
                + "course.durationTime as courseDurationTime, course.lessonsCount as courseLessonsCount, "
                + "course.price as coursePrice, instructor.name as instructorName, "
                + "(select avg(review.rating) from CourseReviewEntity review "
                + "where review.course = course) as courseRating, "
                + "(select count(review) from CourseReviewEntity review "
                + "where review.course = course) as courseReviewsCount "
                + "from UserCourseEntity uc "
                + "join uc.user student "
                + "join uc.course course "
                + "left join course.instructor instructor "
                + "left join AdminEntity studentAdmin on studentAdmin.id = student.id "
                + "where uc.certificateCode = :code and uc.certificateIssued = true")
            Optional<CertificateDetailsView> findIssuedDetailsByCertificateCode(@Param("code") String code);

            /** Projecao agregada de matriculas elegiveis por curso para metricas historicas (US-37). */
            interface CourseEnrollmentCountView {
                UUID getCourseId();

                String getTitle();

                String getCategory();

                long getEnrollments();
            }

            @Query("select course.id as courseId, course.title as title, course.category as category, "
                + "count(uc) as enrollments "
                + "from UserCourseEntity uc "
                + "join uc.course course "
                + "where uc.revokedAt is null "
                + "and course.status <> com.escapa.backend.domain.course.CourseStatus.DRAFT "
                + "group by course.id, course.title, course.category")
            List<CourseEnrollmentCountView> countEligibleEnrollmentsByCourse();
}
