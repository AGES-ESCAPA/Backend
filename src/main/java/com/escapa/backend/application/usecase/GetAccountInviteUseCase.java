package com.escapa.backend.application.usecase;

import java.time.Clock;

import com.escapa.backend.application.model.AccountInviteDetails;
import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.company.CompanyNotFoundException;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.UserNotFoundException;

/** Dados do convite para a tela de criar senha (US-31). Rota pública: quem tem o token vê o convite. */
public class GetAccountInviteUseCase {

    private final AccountInviteValidator accountInviteValidator;
    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;

    public GetAccountInviteUseCase(
            AccountInviteRepositoryPort accountInviteRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            CompanyRepositoryPort companyRepositoryPort,
            Clock clock
    ) {
        this.accountInviteValidator = new AccountInviteValidator(accountInviteRepositoryPort, clock);
        this.userRepositoryPort = userRepositoryPort;
        this.companyRepositoryPort = companyRepositoryPort;
    }

    public AccountInviteDetails execute(String token) {
        final AccountInvite invite = accountInviteValidator.requireOpenInvite(token);
        final User user = userRepositoryPort.findById(invite.getUserId())
                .orElseThrow(() -> new UserNotFoundException(invite.getUserId()));
        final Company company = companyRepositoryPort.findById(invite.getCompanyId())
                .orElseThrow(() -> new CompanyNotFoundException(invite.getCompanyId()));
        return new AccountInviteDetails(user.getName(), user.getEmail(), company.getCompanyName());
    }
}
