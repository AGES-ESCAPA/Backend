package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.CompanyCourseSummary;
import com.escapa.backend.application.model.AcquiredCourse;
import com.escapa.backend.application.port.CompanyCourseRepositoryPort;

import java.util.List;
import java.util.UUID;

public class CompanyCourseRepositoryAdapter implements CompanyCourseRepositoryPort {

    private final CompanyCourseJpaRepository companyCourseJpaRepository;

    public CompanyCourseRepositoryAdapter(CompanyCourseJpaRepository companyCourseJpaRepository) {
        this.companyCourseJpaRepository = companyCourseJpaRepository;
    }

    @Override
    public List<UUID> findAcquiredCourseIds(UUID companyId) {
        return companyCourseJpaRepository.findAcquiredCourseIds(companyId);
    }

    @Override
    public List<AcquiredCourse> findAcquiredCourses(UUID companyId) {
        return companyCourseJpaRepository.findAcquiredCourses(companyId).stream()
                .map(row -> new AcquiredCourse(row.getCourseId(), row.getDeadline()))
                .toList();
    }

    @Override
    public List<CompanyCourseSummary> findActiveCourses(UUID companyId) {
        return companyCourseJpaRepository.findActiveCourses(companyId).stream()
                .map(row -> new CompanyCourseSummary(
                        row.getId(),
                        row.getTitle(),
                        row.getShortDescription(),
                        row.getThumbnailUrl(),
                        row.getCategory(),
                        row.getLevel(),
                        row.getDurationTime(),
                        row.getLessonsCount(),
                        row.getInstructorName(),
                        row.getRating(),
                        row.getAcquiredAt(),
                        row.getEmployeesWithAccess()))
                .toList();
    }
}