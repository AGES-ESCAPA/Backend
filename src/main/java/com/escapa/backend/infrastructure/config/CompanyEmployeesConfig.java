package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.CompanySeatsPort;
import com.escapa.backend.application.usecase.GetCompanySeatsSummaryUseCase;
import com.escapa.backend.application.usecase.ListCompanyEmployeesUseCase;
import com.escapa.backend.infrastructure.persistence.CompanyEmployeeAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Beans do resumo de vagas e da listagem de colaboradores da empresa (US-30). */
@Configuration
public class CompanyEmployeesConfig {

    @Bean
    public CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort(JdbcClient jdbcClient) {
        return new CompanyEmployeeAdapter(jdbcClient);
    }

    @Bean
    public GetCompanySeatsSummaryUseCase getCompanySeatsSummaryUseCase(
            CompanySeatsPort companySeatsPort,
            CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort
    ) {
        return new GetCompanySeatsSummaryUseCase(companySeatsPort, companyEmployeeRepositoryPort);
    }

    @Bean
    public ListCompanyEmployeesUseCase listCompanyEmployeesUseCase(
            CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort
    ) {
        return new ListCompanyEmployeesUseCase(companyEmployeeRepositoryPort);
    }
}
