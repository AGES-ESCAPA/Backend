package com.escapa.backend.infrastructure.persistence;

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
}
