package com.escapa.backend.application.model;

/**
 * Resumo das vagas de uma empresa (US-30). Todo colaborador vinculado ocupa uma vaga, esteja
 * ativo, pendente ou inativo; só desvincular o colaborador libera a vaga.
 *
 * @param seatsTotal      vagas contratadas ({@code company.seats_total})
 * @param seatsUsed       vagas ocupadas, uma por colaborador vinculado
 * @param seatsAvailable  vagas livres; nunca negativo, mesmo que haja mais colaboradores que vagas
 * @param employeesTotal  colaboradores vinculados, de qualquer status
 * @param employeesActive colaboradores com status ACTIVE
 */
public record CompanySeatsSummary(
        int seatsTotal,
        int seatsUsed,
        int seatsAvailable,
        int employeesTotal,
        int employeesActive
) {
}
