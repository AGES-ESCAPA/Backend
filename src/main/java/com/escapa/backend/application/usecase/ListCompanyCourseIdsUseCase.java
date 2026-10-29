package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CompanyCourseRepositoryPort;

import java.util.List;
import java.util.UUID;

/** Ids dos cursos que a empresa já comprou, para o catálogo marcar o curso como adquirido. */
public class ListCompanyCourseIdsUseCase {

    private final CompanyCourseRepositoryPort companyCourseRepositoryPort;

    public ListCompanyCourseIdsUseCase(CompanyCourseRepositoryPort companyCourseRepositoryPort) {
        this.companyCourseRepositoryPort = companyCourseRepositoryPort;
    }

    public List<UUID> execute(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("CompanyId cannot be null");
        }
        return companyCourseRepositoryPort.findAcquiredCourseIds(companyId);
    }
}
