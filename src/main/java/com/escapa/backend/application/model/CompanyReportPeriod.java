package com.escapa.backend.application.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datas de corte do relatório de progresso da equipe (US-35), calculadas pelo use case a partir do
 * relógio no fuso de São Paulo. O adapter só as recebe, então a consulta não depende da hora do banco.
 *
 * @param weekStart        início da janela de 7 dias do engajamento semanal
 * @param quarterStart     primeiro instante do trimestre atual
 * @param previousMonthEnd último dia do mês anterior, base da variação de conclusão
 * @param monthlyFrom      primeiro instante do mês mais antigo da série de horas mensais
 */
public record CompanyReportPeriod(
        LocalDateTime weekStart,
        LocalDateTime quarterStart,
        LocalDate previousMonthEnd,
        LocalDateTime monthlyFrom
) {
}
