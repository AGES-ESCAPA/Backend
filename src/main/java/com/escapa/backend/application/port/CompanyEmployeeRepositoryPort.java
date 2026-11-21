package com.escapa.backend.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.CompanyEmployeeCounts;

/**
 * Colaboradores vinculados a uma empresa em {@code users_company} (US-30). Toda consulta é sempre
 * de uma única empresa: um colaborador de outra empresa nunca aparece.
 */
public interface CompanyEmployeeRepositoryPort {

    /** A empresa tem registro em {@code company}? Um usuário COMPANY pode existir só em {@code users}. */
    boolean existsCompany(UUID companyId);

    /** Vincula o usuário à empresa em {@code users_company}; o vínculo ocupa uma vaga. */
    void linkEmployee(UUID companyId, UUID userId, String jobTitle);

    boolean isEmployeeOfCompany(UUID companyId, UUID userId);

    /** Ids de todos os colaboradores vinculados à empresa, de qualquer status (ACTIVE, PENDING ou INACTIVE). */
    List<UUID> findEmployeeIds(UUID companyId);

    /** O colaborador como aparece na listagem; vazio se não existir ou não for desta empresa. */
    Optional<CompanyEmployee> findEmployee(UUID companyId, UUID userId);

    /** Grava o cargo em {@code users_company.role}. */
    void updateJobTitle(UUID companyId, UUID userId, String jobTitle);

    /** Colaboradores vinculados, de qualquer status, e quantos estão ativos. */
    CompanyEmployeeCounts countEmployees(UUID companyId);

    /**
     * Colaboradores da empresa, paginados e ordenados por nome.
     *
     * @param search trecho do nome ou do e-mail, sem diferenciar maiúsculas; nulo não filtra
     */
    PageResult<CompanyEmployee> findEmployees(UUID companyId, String search, int page, int size);
}
