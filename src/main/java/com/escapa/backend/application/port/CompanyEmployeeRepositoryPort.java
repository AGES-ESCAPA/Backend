package com.escapa.backend.application.port;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.CompanyEmployeeCounts;

import java.util.UUID;

/**
 * Colaboradores vinculados a uma empresa em {@code users_company} (US-30). Toda consulta é sempre
 * de uma única empresa: um colaborador de outra empresa nunca aparece.
 */
public interface CompanyEmployeeRepositoryPort {

    /** Colaboradores vinculados, de qualquer status, e quantos estão ativos. */
    CompanyEmployeeCounts countEmployees(UUID companyId);

    /**
     * Colaboradores da empresa, paginados e ordenados por nome.
     *
     * @param search trecho do nome ou do e-mail, sem diferenciar maiúsculas; nulo não filtra
     */
    PageResult<CompanyEmployee> findEmployees(UUID companyId, String search, int page, int size);
}
