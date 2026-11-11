package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.CompanyCourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.CompanyCourseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CompanyCourseJpaRepository extends JpaRepository<CompanyCourseEntity, CompanyCourseId> {

interface ActiveCourseRow {
        UUID getId();
        String getTitle();
        String getShortDescription();
        String getThumbnailUrl();
        String getCategory();
        String getLevel();
        Integer getDurationTime();
        Integer getLessonsCount();
        String getInstructorName();
        Double getRating();
        LocalDate getAcquiredAt();
        Long getEmployeesWithAccess();
}

@Query("select cc.id.courseId from CompanyCourseEntity cc "
            + "where cc.id.companyId = :companyId and cc.revokedAt is null")
    List<UUID> findAcquiredCourseIds(@Param("companyId") UUID companyId);

@Query("""
        select cc.id.courseId as id,
               c.title as title,
               c.shortDescription as shortDescription,
               c.thumbnailUrl as thumbnailUrl,
               c.category as category,
               c.level as level,
               c.durationTime as durationTime,
               c.lessonsCount as lessonsCount,
               i.name as instructorName,
               c.ratingAverage as rating,
               cc.dataInicio as acquiredAt,
(
    select count(uc.id.userId)
    from UsersCompanyEntity uc
    join UserCourseEntity enrollment
        on enrollment.id.userId = uc.id.userId
    where uc.id.companyId = cc.id.companyId
      and enrollment.id.courseId = cc.id.courseId
      and enrollment.revokedAt is null
) as employeesWithAccess

        from CompanyCourseEntity cc
        join cc.course c
        left join c.instructor i
        where cc.id.companyId = :companyId
          and cc.revokedAt is null
        order by cc.dataInicio desc
        """)

List<ActiveCourseRow> findActiveCourses(@Param("companyId") UUID companyId);


/** Revoga o curso da empresa, sem apagar o registro. */
    @Modifying
    @Query("update CompanyCourseEntity cc set cc.revokedAt = :revokedAt "
            + "where cc.id.companyId = :companyId and cc.id.courseId = :courseId and cc.revokedAt is null")
    int revokeByCompanyIdAndCourseId(
            @Param("companyId") UUID companyId,
            @Param("courseId") UUID courseId,
            @Param("revokedAt") LocalDateTime revokedAt);
}