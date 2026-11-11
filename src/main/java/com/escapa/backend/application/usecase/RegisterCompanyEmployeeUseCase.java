package com.escapa.backend.application.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.escapa.backend.application.model.AcquiredCourse;
import com.escapa.backend.application.model.RegisteredEmployee;
import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.application.port.CompanyCourseRepositoryPort;
import com.escapa.backend.application.port.CompanyEmployeeRepositoryPort;
import com.escapa.backend.application.port.CompanyRepositoryPort;
import com.escapa.backend.application.port.CompanySeatsPort;
import com.escapa.backend.application.port.ContactValidatorPort;
import com.escapa.backend.application.port.OrderAccessPort;
import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.company.CompanyNotFoundException;
import com.escapa.backend.domain.company.NoSeatsAvailableException;
import com.escapa.backend.domain.entity.Company;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.registration.DuplicateRegistrationException;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.domain.user.UserStatus;
import com.escapa.backend.domain.user.UserTypes;

/**
 * Cadastro de colaborador pelo gestor da empresa (US-31). Numa transação só: ocupa uma vaga, cria
 * (ou reativa) a conta PENDING, vincula à empresa, matricula nos cursos da empresa e emite o convite.
 * O e-mail do convite é enviado depois, fora da transação, por {@link EmployeeInviteMailer}: uma falha
 * no envio não pode desfazer o cadastro.
 */
public class RegisterCompanyEmployeeUseCase {

    /** Cargo gravado em {@code users_company.role}: o gestor não informa cargo ao cadastrar. */
    static final String DEFAULT_JOB_TITLE = "COLABORADOR";

    private final UserRepositoryPort userRepositoryPort;
    private final CompanyRepositoryPort companyRepositoryPort;
    private final CompanySeatsPort companySeatsPort;
    private final CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort;
    private final CompanyCourseRepositoryPort companyCourseRepositoryPort;
    private final OrderAccessPort orderAccessPort;
    private final AccountInviteRepositoryPort accountInviteRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;
    private final ContactValidatorPort contactValidatorPort;
    private final Clock clock;
    private final long inviteValidityHours;

    public RegisterCompanyEmployeeUseCase(
            UserRepositoryPort userRepositoryPort,
            CompanyRepositoryPort companyRepositoryPort,
            CompanySeatsPort companySeatsPort,
            CompanyEmployeeRepositoryPort companyEmployeeRepositoryPort,
            CompanyCourseRepositoryPort companyCourseRepositoryPort,
            OrderAccessPort orderAccessPort,
            AccountInviteRepositoryPort accountInviteRepositoryPort,
            PasswordHasherPort passwordHasherPort,
            ContactValidatorPort contactValidatorPort,
            Clock clock,
            long inviteValidityHours
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.companyRepositoryPort = companyRepositoryPort;
        this.companySeatsPort = companySeatsPort;
        this.companyEmployeeRepositoryPort = companyEmployeeRepositoryPort;
        this.companyCourseRepositoryPort = companyCourseRepositoryPort;
        this.orderAccessPort = orderAccessPort;
        this.accountInviteRepositoryPort = accountInviteRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
        this.contactValidatorPort = contactValidatorPort;
        this.clock = clock;
        this.inviteValidityHours = inviteValidityHours;
    }

    public RegisteredEmployee execute(UUID companyId, String name, String email) {
        final String cleanName = requireName(name);
        final String cleanEmail = requireValidEmail(email);

        final Company company = companyRepositoryPort.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
        requireFreeSeat(companyId);

        final User employee = createOrReactivate(cleanName, cleanEmail);
        companyEmployeeRepositoryPort.linkEmployee(companyId, employee.getId(), DEFAULT_JOB_TITLE);
        final int assignedCourses = enrollInCompanyCourses(companyId, employee.getId());

        final String token = AccountInvite.generateToken();
        accountInviteRepositoryPort.save(AccountInvite.issue(
                employee.getId(), companyId, AccountInvite.hashToken(token),
                LocalDateTime.now(clock), inviteValidityHours));

        return new RegisteredEmployee(employee, assignedCourses, company.getCompanyName(), token);
    }

    private String requireName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        final String clean = name.trim();
        if (clean.length() > RegisterStudentUseCase.MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException(
                    "name must have at most " + RegisterStudentUseCase.MAX_FIELD_LENGTH + " characters");
        }
        return clean;
    }

    private String requireValidEmail(String email) {
        if (email == null || email.trim().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        final String clean = email.trim().toLowerCase(Locale.ROOT);
        if (clean.length() > RegisterStudentUseCase.MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException(
                    "email must have at most " + RegisterStudentUseCase.MAX_FIELD_LENGTH + " characters");
        }
        if (!contactValidatorPort.isValidEmail(clean)) {
            throw new IllegalArgumentException("Invalid email");
        }
        return clean;
    }

    /** Trava a empresa antes de contar: dois cadastros ao mesmo tempo não ocupam a mesma última vaga. */
    private void requireFreeSeat(UUID companyId) {
        final int seatsTotal = companySeatsPort.findSeatsTotalForUpdate(companyId);
        if (companySeatsPort.countSeatsInUse(companyId) >= seatsTotal) {
            throw new NoSeatsAvailableException(seatsTotal);
        }
    }

    private User createOrReactivate(String name, String email) {
        final Optional<User> existing = userRepositoryPort.findByEmail(email);
        if (existing.isEmpty()) {
            return userRepositoryPort.save(
                    new User(null, name, email, unusablePasswordHash(), UserTypes.STUDENT, null,
                            UserStatus.PENDING, LocalDateTime.now(clock)));
        }
        final User former = existing.get();
        if (!isFormerEmployee(former)) {
            throw new DuplicateRegistrationException("email");
        }
        // A conta antiga volta a ser PENDING: a senha antiga deixa de valer até o colaborador criar outra.
        former.setStatus(UserStatus.PENDING);
        former.setPasswordHash(unusablePasswordHash());
        return userRepositoryPort.save(former);
    }

    /** Ex-colaborador: conta inativa, sem vínculo com nenhuma empresa e que já recebeu convite. */
    private boolean isFormerEmployee(User user) {
        return user.getStatus() == UserStatus.INACTIVE
                && !userRepositoryPort.isUserLinkedToCompany(user.getId())
                && accountInviteRepositoryPort.existsByUserId(user.getId());
    }

    /** Hash de uma senha aleatória que ninguém conhece: a conta só ganha senha pelo link do convite. */
    private String unusablePasswordHash() {
        return passwordHasherPort.hash(AccountInvite.generateToken());
    }

    /**
     * Matricula o colaborador em cada curso da empresa, com as regras da compra (#48): começa hoje e
     * expira em hoje + prazo do curso; matrícula revogada é reativada mantendo o progresso.
     */
    private int enrollInCompanyCourses(UUID companyId, UUID employeeId) {
        final LocalDate today = LocalDate.now(clock);
        final List<AcquiredCourse> courses = companyCourseRepositoryPort.findAcquiredCourses(companyId);
        for (final AcquiredCourse course : courses) {
            final LocalDate expiration = course.deadline() == null ? null : today.plusDays(course.deadline());
            orderAccessPort.saveStudentEnrollment(employeeId, course.courseId(), today, expiration);
        }
        return courses.size();
    }
}
