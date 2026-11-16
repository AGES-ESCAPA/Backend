package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CompanySeatsSummary;
import com.escapa.backend.application.port.CompanyEmployeeUnlinkPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.company.CompanyEmployeeNotFoundException;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Desvincula um colaborador desligado (US-33): remove o vínculo e libera a vaga, revoga as matrículas
 * nos cursos da empresa, inativa a conta e invalida os convites em aberto. Progresso, matrículas e
 * certificados não são apagados. Roda em uma única transação: uma falha em qualquer passo desfaz tudo.
 */
public class UnlinkCompanyEmployeeUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnlinkCompanyEmployeeUseCase.class);

    private final CompanyEmployeeUnlinkPort unlinkPort;
    private final UserRepositoryPort userRepositoryPort;
    private final GetCompanySeatsSummaryUseCase getCompanySeatsSummaryUseCase;
    private final Clock clock;

    public UnlinkCompanyEmployeeUseCase(
            CompanyEmployeeUnlinkPort unlinkPort,
            UserRepositoryPort userRepositoryPort,
            GetCompanySeatsSummaryUseCase getCompanySeatsSummaryUseCase,
            Clock clock
    ) {
        this.unlinkPort = unlinkPort;
        this.userRepositoryPort = userRepositoryPort;
        this.getCompanySeatsSummaryUseCase = getCompanySeatsSummaryUseCase;
        this.clock = clock;
    }

    /** Devolve o resumo de vagas já atualizado. */
    public CompanySeatsSummary execute(UUID companyId, UUID employeeId) {
        if (companyId == null || employeeId == null) {
            throw new IllegalArgumentException("Company and employee are required");
        }
        final LocalDateTime now = LocalDateTime.now(clock);

        // Colaborador de outra empresa (ou inexistente) cai aqui antes de qualquer outra alteração.
        if (!unlinkPort.removeLink(companyId, employeeId)) {
            throw new CompanyEmployeeNotFoundException(companyId, employeeId);
        }
        final int revoked = unlinkPort.revokeCompanyCourseEnrollments(companyId, employeeId, now);
        unlinkPort.invalidatePendingInvites(companyId, employeeId, now);
        inactivate(employeeId);

        LOGGER.info("Employee {} unlinked from company {} ({} enrollments revoked)", employeeId, companyId, revoked);
        return getCompanySeatsSummaryUseCase.execute(companyId);
    }

    private void inactivate(UUID employeeId) {
        final User employee = userRepositoryPort.findById(employeeId)
                .orElseThrow(() -> new UserNotFoundException(employeeId));
        if (employee.getStatus() != UserStatus.INACTIVE) {
            employee.setStatus(UserStatus.INACTIVE);
            userRepositoryPort.save(employee);
        }
    }
}
