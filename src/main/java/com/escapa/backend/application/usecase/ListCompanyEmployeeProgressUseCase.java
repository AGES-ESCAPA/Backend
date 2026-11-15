package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployeeProgress;
import com.escapa.backend.application.model.EmployeeProgressStatus;
import com.escapa.backend.application.port.CompanyReportRepositoryPort;
import com.escapa.backend.domain.company.CompanyNotFoundException;

import java.util.UUID;

/**
 * Progresso de cada colaborador em cada curso da empresa (US-35): uma linha por colaborador e curso,
 * paginada, com busca opcional por nome e filtro opcional de situação. A empresa é sempre a do token.
 */
public class ListCompanyEmployeeProgressUseCase {

    private static final char NUL = '\u0000';

    private final CompanyReportRepositoryPort companyReportRepositoryPort;

    public ListCompanyEmployeeProgressUseCase(CompanyReportRepositoryPort companyReportRepositoryPort) {
        this.companyReportRepositoryPort = companyReportRepositoryPort;
    }

    /**
     * @param search busca por nome; nula ou em branco não filtra
     * @param status situação da matrícula; nulo não filtra
     * @throws IllegalArgumentException paginação fora dos limites ou busca com o caractere NUL
     * @throws CompanyNotFoundException se o usuário COMPANY do token não tem registro em {@code company}
     */
    public PageResult<CompanyEmployeeProgress> execute(
            UUID companyId, String search, EmployeeProgressStatus status, int page, int size) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId cannot be null");
        }
        PaginationRules.requireValid(page, size);
        final String term = normalizeSearch(search);

        if (!companyReportRepositoryPort.existsCompany(companyId)) {
            throw new CompanyNotFoundException(companyId);
        }
        return companyReportRepositoryPort.findEmployeeProgress(companyId, term, status, page, size);
    }

    /**
     * Mesma regra da busca de colaboradores (US-30): {@code strip()} tira também os espaços Unicode, a
     * busca vazia não filtra e o NUL é recusado, porque o Postgres não o aceita em texto.
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
