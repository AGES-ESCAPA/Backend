package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.domain.company.CompanyNotFoundException;

import java.util.UUID;

/**
 * Lista os colaboradores vinculados à empresa (US-30), paginados e ordenados por nome, com busca
 * opcional por nome ou e-mail. A empresa é sempre a do token.
 */
public class ListCompanyEmployeesUseCase {

    private static final char NUL = '\u0000';

    private final CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort;

    public ListCompanyEmployeesUseCase(CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort) {
        this.companyEmployeeRepositoryPort = companyEmployeeRepositoryPort;
    }

    /**
     * @throws IllegalArgumentException paginação fora dos limites ou busca com o caractere NUL
     * @throws CompanyNotFoundException se o usuário COMPANY do token não tem registro em {@code company}
     */
    public PageResult<CompanyEmployee> execute(UUID companyId, String search, int page, int size) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId cannot be null");
        }
        PaginationRules.requireValid(page, size);
        final String term = normalizeSearch(search);

        if (!companyEmployeeRepositoryPort.existsCompany(companyId)) {
            throw new CompanyNotFoundException(companyId);
        }
        return companyEmployeeRepositoryPort.findEmployees(companyId, term, page, size);
    }

    /**
     * {@code strip()} tira também os espaços Unicode (como o ideográfico U+3000), que {@code trim()}
     * deixa. Busca vazia não filtra. O NUL é recusado: o Postgres não o aceita em texto e a consulta
     * estouraria como erro 500.
     */
    private static String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        if (search.indexOf(NUL) >= 0) {
            throw new IllegalArgumentException("Invalid search");
        }
        final String stripped = search.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
