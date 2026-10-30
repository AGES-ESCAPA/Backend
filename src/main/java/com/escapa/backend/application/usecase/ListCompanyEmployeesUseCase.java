package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;

import java.util.UUID;

/**
 * Lista os colaboradores vinculados à empresa (US-30), paginados e ordenados por nome, com busca
 * opcional por nome ou e-mail. A empresa é sempre a do token.
 */
public class ListCompanyEmployeesUseCase {

    public static final int MAX_PAGE_SIZE = 100;

    private final CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort;

    public ListCompanyEmployeesUseCase(CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort) {
        this.companyEmployeeRepositoryPort = companyEmployeeRepositoryPort;
    }

    public PageResult<CompanyEmployee> execute(UUID companyId, String search, int page, int size) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId cannot be null");
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid pagination");
        }

        final String term = search == null || search.isBlank() ? null : search.trim();
        return companyEmployeeRepositoryPort.findEmployees(companyId, term, page, size);
    }
}
