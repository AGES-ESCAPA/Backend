package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CompanyEmployeeCounts;
import com.escapa.backend.application.model.CompanySeatsSummary;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.CompanySeatsPort;
import com.escapa.backend.domain.company.CompanyNotFoundException;

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

    /** @throws CompanyNotFoundException se o usuário COMPANY do token não tem registro em {@code company} */
    public CompanySeatsSummary execute(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId cannot be null");
        }
        if (!companyEmployeeRepositoryPort.existsCompany(companyId)) {
            throw new CompanyNotFoundException(companyId);
        }

        final int seatsTotal = companySeatsPort.findSeatsTotal(companyId);
        final CompanyEmployeeCounts employees = companyEmployeeRepositoryPort.countEmployees(companyId);

        // Todo colaborador vinculado ocupa uma vaga, então as vagas em uso são os colaboradores. Uma
        // única contagem alimenta os dois números e eles nunca divergem, mesmo com convite ou
        // desvínculo no meio da leitura.
        final int seatsUsed = employees.total();

        // Com mais colaboradores que vagas (dado antigo ou vaga reduzida), as livres ficam em zero.
        final int seatsAvailable = Math.max(0, seatsTotal - seatsUsed);

        return new CompanySeatsSummary(seatsTotal, seatsUsed, seatsAvailable, employees.total(), employees.active());
    }
}
