package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CompanyEmployeeCounts;
import com.escapa.backend.application.model.CompanySeatsSummary;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.CompanySeatsPort;

import java.util.UUID;

/**
 * Resumo das vagas contratadas e em uso da empresa (US-30). A empresa é sempre a do token; nenhuma
 * entrada traz o id de outra empresa.
 */
public class GetCompanySeatsSummaryUseCase {

    private final CompanySeatsPort companySeatsPort;
    private final CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort;

    public GetCompanySeatsSummaryUseCase(
            CompanySeatsPort companySeatsPort,
            CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort
    ) {
        this.companySeatsPort = companySeatsPort;
        this.companyEmployeeRepositoryPort = companyEmployeeRepositoryPort;
    }

    public CompanySeatsSummary execute(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId cannot be null");
        }

        final int seatsTotal = companySeatsPort.findSeatsTotal(companyId);
        final int seatsUsed = companySeatsPort.countSeatsInUse(companyId);
        final CompanyEmployeeCounts employees = companyEmployeeRepositoryPort.countEmployees(companyId);

        // Com mais colaboradores que vagas (dado antigo ou vaga reduzida), as livres ficam em zero.
        final int seatsAvailable = Math.max(0, seatsTotal - seatsUsed);

        return new CompanySeatsSummary(seatsTotal, seatsUsed, seatsAvailable, employees.total(), employees.active());
    }
}
