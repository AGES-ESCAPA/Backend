package com.escapa.backend.application.model;

import java.time.YearMonth;
import java.util.List;

/**
 * Contagens cruas do panorama da empresa (US-35). As taxas, médias e o arredondamento são calculados
 * pelo use case; aqui só há o que o banco consegue contar.
 *
 * @param activeEmployees               colaboradores ACTIVE vinculados à empresa
 * @param enrollments                   matrículas não revogadas nos cursos da empresa
 * @param completedEnrollments          dessas, as concluídas
 * @param completedMinutes              minutos das aulas concluídas pelos colaboradores nesses cursos
 * @param certificatesThisQuarter       certificados emitidos desde o início do trimestre
 * @param activeLastWeek                colaboradores ACTIVE com acesso na última semana
 * @param enrollmentsAtPreviousMonthEnd matrículas que já tinham começado no fim do mês anterior
 * @param completedAtPreviousMonthEnd   dessas, as concluídas até o fim do mês anterior
 * @param byJobTitle                    matrículas e conclusões por cargo
 * @param monthlyMinutes                minutos concluídos por mês; só meses com dados
 */
public record CompanyReportFigures(
        long activeEmployees,
        long enrollments,
        long completedEnrollments,
        long completedMinutes,
        long certificatesThisQuarter,
        long activeLastWeek,
        long enrollmentsAtPreviousMonthEnd,
        long completedAtPreviousMonthEnd,
        List<JobTitleFigures> byJobTitle,
        List<MonthFigures> monthlyMinutes
) {

    public record JobTitleFigures(String jobTitle, long enrollments, long completed) {
    }

    public record MonthFigures(YearMonth month, long minutes) {
    }
}
