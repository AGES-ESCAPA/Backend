package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CompanyEmployeeResponse;
import com.escapa.backend.adapters.dto.CompanySeatsResponse;
import com.escapa.backend.adapters.dto.CreateCompanyEmployeeRequest;
import com.escapa.backend.adapters.dto.PageResponse;
import com.escapa.backend.adapters.dto.RegisteredEmployeeResponse;
import com.escapa.backend.adapters.dto.ResentInviteResponse;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.IssuedEmployeeInvite;
import com.escapa.backend.application.model.RegisteredEmployee;
import com.escapa.backend.application.usecase.EmployeeInviteMailer;
import com.escapa.backend.application.usecase.GetCompanySeatsSummaryUseCase;
import com.escapa.backend.application.usecase.ListCompanyEmployeesUseCase;
import com.escapa.backend.application.usecase.RegisterCompanyEmployeeUseCase;
import com.escapa.backend.application.usecase.ResendEmployeeInviteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Gestão de Colaboradores da empresa autenticada (US-30). A empresa é sempre a do token
 * ({@code company.id} é o mesmo id do usuário); nenhuma rota recebe o id da empresa.
 */
@RestController
@Tag(name = "Company - Employees", description = "Seats and employees of the authenticated company")
@RequestMapping("/api/v1/company")
public class CompanyEmployeeController {

    private final GetCompanySeatsSummaryUseCase getCompanySeatsSummaryUseCase;
    private final ListCompanyEmployeesUseCase listCompanyEmployeesUseCase;
    private final RegisterCompanyEmployeeUseCase registerCompanyEmployeeUseCase;
    private final ResendEmployeeInviteUseCase resendEmployeeInviteUseCase;
    private final EmployeeInviteMailer employeeInviteMailer;

    public CompanyEmployeeController(
            GetCompanySeatsSummaryUseCase getCompanySeatsSummaryUseCase,
            ListCompanyEmployeesUseCase listCompanyEmployeesUseCase,
            RegisterCompanyEmployeeUseCase registerCompanyEmployeeUseCase,
            ResendEmployeeInviteUseCase resendEmployeeInviteUseCase,
            EmployeeInviteMailer employeeInviteMailer
    ) {
        this.getCompanySeatsSummaryUseCase = getCompanySeatsSummaryUseCase;
        this.listCompanyEmployeesUseCase = listCompanyEmployeesUseCase;
        this.registerCompanyEmployeeUseCase = registerCompanyEmployeeUseCase;
        this.resendEmployeeInviteUseCase = resendEmployeeInviteUseCase;
        this.employeeInviteMailer = employeeInviteMailer;
    }

    @GetMapping("/seats")
    @Operation(
            summary = "Summary of the seats of the company",
            description = "Seats contracted and in use, and the employees linked to the company. Every linked "
                    + "employee takes a seat, whether ACTIVE, PENDING or INACTIVE; only unlinking frees it.")
    public ResponseEntity<ApiResponse<CompanySeatsResponse>> getSeats() {
        final UUID companyId = AuthenticationContext.requireUser().id();
        return ResponseEntity.ok(ApiResponse.success(
                CompanySeatsResponse.from(getCompanySeatsSummaryUseCase.execute(companyId))));
    }

    @GetMapping("/employees")
    @Operation(
            summary = "List the employees of the company",
            description = "Paginated and ordered by name. The optional search filters by name or e-mail, "
                    + "ignoring case. Only employees of the company of the token are returned.")
    public ResponseEntity<ApiResponse<PageResponse<CompanyEmployeeResponse>>> listEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = PaginationDefaults.FIRST_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.PAGE_SIZE) int size
    ) {
        final UUID companyId = AuthenticationContext.requireUser().id();
        final PageResult<CompanyEmployee> result = listCompanyEmployeesUseCase.execute(companyId, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result, CompanyEmployeeResponse::from)));
    }

    @PostMapping("/employees")
    @Operation(
            summary = "Register an employee of the company",
            description = "Takes a seat, creates the PENDING account (or reactivates a former employee), enrolls "
                    + "the employee in the courses of the company and e-mails the link to create the password. "
                    + "No free seat or an e-mail already registered returns 409. A failure to send the e-mail "
                    + "does not undo the registration: the response has inviteSent=false and the invite can be resent.")
    public ResponseEntity<ApiResponse<RegisteredEmployeeResponse>> registerEmployee(
            @RequestBody CreateCompanyEmployeeRequest request
    ) {
        final UUID companyId = AuthenticationContext.requireUser().id();
        final RegisteredEmployee registered =
                registerCompanyEmployeeUseCase.execute(companyId, request.name(), request.email());
        // Fora da transação do cadastro: o use case já confirmou antes de o e-mail sair.
        final boolean inviteSent = employeeInviteMailer.send(
                registered.employee().getName(), registered.employee().getEmail(),
                registered.companyName(), registered.inviteToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                RegisteredEmployeeResponse.from(registered, inviteSent),
                "Colaborador cadastrado com sucesso! O mesmo receberá um e-mail para criação de senha no e-mail cadastrado"));
    }

    @PostMapping("/employees/{id}/invite")
    @Operation(
            summary = "Resend the invite of a pending employee",
            description = "Invalidates the previous invites and sends a new link. An employee that is not PENDING "
                    + "returns 409; an employee of another company returns 404.")
    public ResponseEntity<ApiResponse<ResentInviteResponse>> resendInvite(@PathVariable UUID id) {
        final UUID companyId = AuthenticationContext.requireUser().id();
        final IssuedEmployeeInvite issued = resendEmployeeInviteUseCase.execute(companyId, id);
        final boolean inviteSent = employeeInviteMailer.send(
                issued.employee().getName(), issued.employee().getEmail(),
                issued.companyName(), issued.inviteToken());
        return ResponseEntity.ok(ApiResponse.success(
                new ResentInviteResponse(issued.employee().getId(), issued.employee().getEmail(), inviteSent),
                inviteSent ? "Convite reenviado com sucesso" : "Não foi possível enviar o e-mail do convite. Tente novamente"));
    }
}
