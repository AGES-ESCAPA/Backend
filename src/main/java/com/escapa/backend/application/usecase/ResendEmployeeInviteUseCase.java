package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.application.model.IssuedEmployeeInvite;
import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.company.CompanyNotFoundException;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.EmployeeNotPendingException;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.domain.user.UserStatus;

/**
 * Reenvio do convite a um colaborador PENDING (US-31). O banco guarda só o hash do token, então o
 * link antigo não dá para reaproveitar: os convites em aberto são encerrados e um novo é emitido.
 * O e-mail é enviado depois, fora da transação, por {@link EmployeeInviteMailer}.
 */
public class ResendEmployeeInviteUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;
    private final CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort;
    private final AccountInviteRepositoryPort accountInviteRepositoryPort;
    private final Clock clock;
    private final long inviteValidityHours;

    public ResendEmployeeInviteUseCase(
            UserRepositoryPort userRepositoryPort,
            CompanyRepositoryPort companyRepositoryPort,
            CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort,
            AccountInviteRepositoryPort accountInviteRepositoryPort,
            Clock clock,
            long inviteValidityHours
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.companyRepositoryPort = companyRepositoryPort;
        this.companyEmployeeRepositoryPort = companyEmployeeRepositoryPort;
        this.accountInviteRepositoryPort = accountInviteRepositoryPort;
        this.clock = clock;
        this.inviteValidityHours = inviteValidityHours;
    }

    public IssuedEmployeeInvite execute(UUID companyId, UUID employeeId) {
        // Colaborador de outra empresa é "não encontrado", para não revelar que ele existe.
        if (!companyEmployeeRepositoryPort.isEmployeeOfCompany(companyId, employeeId)) {
            throw new UserNotFoundException(employeeId);
        }
        final User employee = userRepositoryPort.findById(employeeId)
                .orElseThrow(() -> new UserNotFoundException(employeeId));
        if (employee.getStatus() != UserStatus.PENDING) {
            throw new EmployeeNotPendingException(employeeId);
        }
        final String companyName = companyRepositoryPort.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId))
                .getCompanyName();

        final LocalDateTime now = LocalDateTime.now(clock);
        accountInviteRepositoryPort.invalidateOpenInvites(employeeId, now);
        final String token = AccountInvite.generateToken();
        accountInviteRepositoryPort.save(
                AccountInvite.issue(employeeId, companyId, AccountInvite.hashToken(token), now, inviteValidityHours));
        return new IssuedEmployeeInvite(employee, companyName, token);
    }
}
