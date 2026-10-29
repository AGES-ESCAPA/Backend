package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.infrastructure.persistence.entity.CompanyCourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.CompanyCourseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CompanyCourseJpaRepository extends JpaRepository<CompanyCourseEntity, CompanyCourseId> {

    @Query("select cc.id.courseId from CompanyCourseEntity cc "
            + "where cc.id.companyId = :companyId and cc.revokedAt is null")
    List<UUID> findAcquiredCourseIds(@Param("companyId") UUID companyId);

    /** Revoga o curso da empresa, sem apagar o registro. */
    @Modifying
    @Query("update CompanyCourseEntity cc set cc.revokedAt = :revokedAt "
            + "where cc.id.companyId = :companyId and cc.id.courseId = :courseId and cc.revokedAt is null")
    int revokeByCompanyIdAndCourseId(
            @Param("companyId") UUID companyId,
            @Param("courseId") UUID courseId,
            @Param("revokedAt") LocalDateTime revokedAt);
}
