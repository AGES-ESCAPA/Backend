package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.UpdatedEmployee;
import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.application.port.ContactValidatorPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.company.CompanyEmployeeNotFoundException;
import com.escapa.backend.domain.company.CompanyNotFoundException;
import com.escapa.backend.domain.company.JobTitleNormalizer;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.UserStatus;

/**
 * Edição de colaborador pelo gestor (US-32): nome, e-mail, cargo e status do acesso. O vínculo, a vaga
 * e as matrículas nunca mudam aqui; só a desvinculação (US-33) libera a vaga.
 *
 * <ul>
 *   <li>{@code INACTIVE} bloqueia a entrada pelas regras de conta inativa do login e do filtro de
 *       autenticação; um convite em aberto é invalidado.</li>
 *   <li>{@code ACTIVE} para quem ainda não tem senha mantém a conta {@code PENDING}: sem senha não há
 *       como entrar. Isso vale para o pendente e para quem foi inativado antes de criar a senha; este
 *       último recebe um convite novo, porque o anterior foi invalidado ao inativar.</li>
 *   <li>Trocar o e-mail de um pendente invalida o convite anterior e emite outro para o e-mail novo.</li>
 * </ul>
 *
 * <p>O e-mail do convite é enviado depois, fora da transação, por {@link EmployeeInviteMailer}.
 */
public class UpdateCompanyEmployeeUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;
    private final CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort;
    private final AccountInviteRepositoryPort accountInviteRepositoryPort;
    private final EmployeeDataValidator validator;
    private final Clock clock;
    private final long inviteValidityHours;

    public UpdateCompanyEmployeeUseCase(
            UserRepositoryPort userRepositoryPort,
            CompanyRepositoryPort companyRepositoryPort,
            CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort,
            AccountInviteRepositoryPort accountInviteRepositoryPort,
            ContactValidatorPort contactValidatorPort,
            Clock clock,
            long inviteValidityHours
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.companyRepositoryPort = companyRepositoryPort;
        this.companyEmployeeRepositoryPort = companyEmployeeRepositoryPort;
        this.accountInviteRepositoryPort = accountInviteRepositoryPort;
        this.validator = new EmployeeDataValidator(contactValidatorPort);
        this.clock = clock;
        this.inviteValidityHours = inviteValidityHours;
    }

    public UpdatedEmployee execute(UUID companyId, UUID employeeId, EmployeeChanges changes) {
        final String name = validator.requireName(changes.name());
        final String email = validator.requireValidEmail(changes.email());
        final String requestedJobTitle = validator.requireJobTitle(changes.jobTitle());
        final UserStatus requestedStatus = requireEditableStatus(changes.status());

        // Colaborador de outra empresa é "não encontrado", para não revelar que ele existe.
        final CompanyEmployee current = companyEmployeeRepositoryPort.findEmployee(companyId, employeeId)
                .orElseThrow(() -> new CompanyEmployeeNotFoundException(companyId, employeeId));
        final User user = userRepositoryPort.findById(employeeId)
                .orElseThrow(() -> new CompanyEmployeeNotFoundException(companyId, employeeId));
        requireEmailAvailable(email, employeeId);

        final String jobTitle = JobTitleNormalizer.resolveForCompany(
                requestedJobTitle, companyEmployeeRepositoryPort.findJobTitles(companyId));
        final UserStatus previousStatus = user.getStatus();
        final boolean emailChanged = !email.equals(user.getEmail());
        final UserStatus newStatus = resolveStatus(user, requestedStatus);

        user.setName(name);
        user.setEmail(email);
        user.setStatus(newStatus);
        userRepositoryPort.save(user);
        companyEmployeeRepositoryPort.updateJobTitle(companyId, employeeId, jobTitle);

        final String token = handleInvites(companyId, employeeId, previousStatus, newStatus, emailChanged);
        final CompanyEmployee updated = new CompanyEmployee(employeeId, name, email, jobTitle, newStatus,
                current.assignedCourses(), current.avatarUrl());
        return new UpdatedEmployee(updated, token == null ? null : companyName(companyId), token);
    }

    /** O gestor só escolhe entre ativo e inativo; {@code PENDING} é consequência de ainda não ter senha. */
    private static UserStatus requireEditableStatus(UserStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
        if (status != UserStatus.ACTIVE && status != UserStatus.INACTIVE) {
            throw new IllegalArgumentException("status must be ACTIVE or INACTIVE");
        }
        return status;
    }

    private void requireEmailAvailable(String email, UUID employeeId) {
        userRepositoryPort.findByEmail(email)
                .filter(owner -> !owner.getId().equals(employeeId))
                .ifPresent(owner -> {
                    throw new DuplicateRegistrationException("email");
                });
    }

    private UserStatus resolveStatus(User user, UserStatus requested) {
        if (requested == UserStatus.INACTIVE) {
            return UserStatus.INACTIVE;
        }
        return hasPassword(user) ? UserStatus.ACTIVE : UserStatus.PENDING;
    }

    /**
     * Pendente nunca criou a senha. Um inativo pode ter sido inativado ainda pendente: nesse caso o
     * último convite foi encerrado sem uso e a senha guardada é a aleatória do cadastro.
     */
    private boolean hasPassword(User user) {
        return switch (user.getStatus()) {
            case ACTIVE -> true;
            case PENDING -> false;
            case INACTIVE -> accountInviteRepositoryPort.isLatestInviteUsed(user.getId());
        };
    }

    /** @return o token do convite novo, ou nulo quando a edição não pede convite */
    private String handleInvites(UUID companyId, UUID employeeId, UserStatus previousStatus,
                                 UserStatus newStatus, boolean emailChanged) {
        final LocalDateTime now = LocalDateTime.now(clock);
        if (newStatus == UserStatus.INACTIVE) {
            accountInviteRepositoryPort.invalidateOpenInvites(employeeId, now);
            return null;
        }
        final boolean needsInvite = newStatus == UserStatus.PENDING
                && (emailChanged || previousStatus != UserStatus.PENDING);
        if (!needsInvite) {
            return null;
        }
        accountInviteRepositoryPort.invalidateOpenInvites(employeeId, now);
        final String token = AccountInvite.generateToken();
        accountInviteRepositoryPort.save(
                AccountInvite.issue(employeeId, companyId, AccountInvite.hashToken(token), now, inviteValidityHours));
        return token;
    }

    private String companyName(UUID companyId) {
        return companyRepositoryPort.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId))
                .getCompanyName();
    }

    /** Dados pedidos pelo gestor, ainda sem validação. */
    public record EmployeeChanges(String name, String email, String jobTitle, UserStatus status) {
    }
}
