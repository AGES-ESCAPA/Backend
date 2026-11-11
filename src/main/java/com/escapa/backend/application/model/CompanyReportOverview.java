package com.escapa.backend.application.model;

import java.time.YearMonth;
import java.util.List;

/**
 * Indicadores de conclusão e engajamento da equipe de uma empresa (US-35).
 *
 * @param completionRate          % das matrículas concluídas, com uma casa decimal
 * @param completionRateChange    variação da taxa de conclusão em pontos percentuais desde o fim do
 *                                mês anterior; 0 quando a empresa ainda não tinha matrículas então
 * @param averageHoursPerEmployee horas de aulas concluídas por colaborador ativo
 * @param certificatesThisQuarter certificados emitidos no trimestre atual
 * @param weeklyEngagement        % dos colaboradores ativos com acesso nos últimos 7 dias
 * @param completionByJobTitle    taxa de conclusão por cargo, em ordem alfabética
 * @param monthlyHours            sempre 6 meses, do mais antigo ao atual, com 0 nos meses sem aulas
 */
public record CompanyReportOverview(
        double completionRate,
        double completionRateChange,
        double averageHoursPerEmployee,
        long certificatesThisQuarter,
        double weeklyEngagement,
        List<JobTitleCompletion> completionByJobTitle,
        List<MonthlyHours> monthlyHours
) {

    public record JobTitleCompletion(String jobTitle, double completionRate) {
    }

    public record MonthlyHours(YearMonth month, double hours) {
    }
}
