package com.escapa.backend.application.port;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployeeProgress;
import com.escapa.backend.application.model.CompanyReportFigures;
import com.escapa.backend.application.model.CompanyReportPeriod;
import com.escapa.backend.application.model.EmployeeProgressStatus;

import java.util.UUID;

/**
 * Leituras do relatório de progresso da equipe (US-35). Toda consulta é de uma única empresa e conta
 * só matrículas não revogadas, nos cursos que a empresa tem e não teve revogados.
 */
public interface CompanyReportRepositoryPort {

    /** A empresa tem registro em {@code company}? Um usuário COMPANY pode existir só em {@code users}. */
    boolean existsCompany(UUID companyId);

    /** Contagens do panorama; empresa sem colaboradores devolve tudo zerado e listas vazias. */
    CompanyReportFigures loadFigures(UUID companyId, CompanyReportPeriod period);

    /**
     * Uma linha por colaborador e curso, do acesso mais recente para o mais antigo (quem nunca
     * acessou vai por último), com desempate por nome.
     *
     * @param search trecho do nome, sem diferenciar maiúsculas nem acentos de ordenação; nulo não filtra
     * @param status nulo não filtra
     */
    PageResult<CompanyEmployeeProgress> findEmployeeProgress(
            UUID companyId, String search, EmployeeProgressStatus status, int page, int size);
}
