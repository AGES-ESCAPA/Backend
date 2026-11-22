package com.escapa.backend.adapters.controller;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CompanyEmployeeResponse;
import com.escapa.backend.adapters.dto.CompanySeatsResponse;
import com.escapa.backend.adapters.dto.CreateCompanyEmployeeRequest;
import com.escapa.backend.adapters.dto.EmployeeCourseAssignmentResponse;
import com.escapa.backend.adapters.dto.PageResponse;
import com.escapa.backend.adapters.dto.RegisteredEmployeeResponse;
import com.escapa.backend.adapters.dto.ResentInviteResponse;
import com.escapa.backend.adapters.dto.UpdateCompanyEmployeeRequest;
import com.escapa.backend.adapters.dto.UpdatedEmployeeResponse;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.model.IssuedEmployeeInvite;
import com.escapa.backend.application.model.RegisteredEmployee;
import com.escapa.backend.application.model.UpdatedEmployee;
import com.escapa.backend.application.usecase.AssignEmployeeCourseUseCase;
import com.escapa.backend.application.usecase.EmployeeInviteMailer;
import com.escapa.backend.application.usecase.GetCompanySeatsSummaryUseCase;
import com.escapa.backend.application.usecase.ListCompanyEmployeesUseCase;
import com.escapa.backend.application.usecase.ListEmployeeCoursesUseCase;
import com.escapa.backend.application.usecase.RegisterCompanyEmployeeUseCase;
import com.escapa.backend.application.usecase.ResendEmployeeInviteUseCase;
import com.escapa.backend.application.usecase.RevokeEmployeeCourseUseCase;
import com.escapa.backend.application.usecase.UnlinkCompanyEmployeeUseCase;
import com.escapa.backend.application.usecase.UpdateCompanyEmployeeUseCase;
import com.escapa.backend.application.usecase.UpdateCompanyEmployeeUseCase.EmployeeChanges;

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
    private final UnlinkCompanyEmployeeUseCase unlinkCompanyEmployeeUseCase;
    private final UpdateCompanyEmployeeUseCase updateCompanyEmployeeUseCase;
    private final ListEmployeeCoursesUseCase listEmployeeCoursesUseCase;
    private final AssignEmployeeCourseUseCase assignEmployeeCourseUseCase;
    private final RevokeEmployeeCourseUseCase revokeEmployeeCourseUseCase;

    public CompanyEmployeeController(
            GetCompanySeatsSummaryUseCase getCompanySeatsSummaryUseCase,
            ListCompanyEmployeesUseCase listCompanyEmployeesUseCase,
            RegisterCompanyEmployeeUseCase registerCompanyEmployeeUseCase,
            ResendEmployeeInviteUseCase resendEmployeeInviteUseCase,
            EmployeeInviteMailer employeeInviteMailer,
            UnlinkCompanyEmployeeUseCase unlinkCompanyEmployeeUseCase,
            UpdateCompanyEmployeeUseCase updateCompanyEmployeeUseCase,
            ListEmployeeCoursesUseCase listEmployeeCoursesUseCase,
            AssignEmployeeCourseUseCase assignEmployeeCourseUseCase,
            RevokeEmployeeCourseUseCase revokeEmployeeCourseUseCase
    ) {
        this.getCompanySeatsSummaryUseCase = getCompanySeatsSummaryUseCase;
        this.listCompanyEmployeesUseCase = listCompanyEmployeesUseCase;
        this.registerCompanyEmployeeUseCase = registerCompanyEmployeeUseCase;
        this.resendEmployeeInviteUseCase = resendEmployeeInviteUseCase;
        this.employeeInviteMailer = employeeInviteMailer;
        this.unlinkCompanyEmployeeUseCase = unlinkCompanyEmployeeUseCase;
        this.updateCompanyEmployeeUseCase = updateCompanyEmployeeUseCase;
        this.listEmployeeCoursesUseCase = listEmployeeCoursesUseCase;
        this.assignEmployeeCourseUseCase = assignEmployeeCourseUseCase;
        this.revokeEmployeeCourseUseCase = revokeEmployeeCourseUseCase;
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

    @PutMapping("/employees/{id}")
    @Operation(
            summary = "Edit an employee of the company",
            description = "Saves name, e-mail, job title and status (ACTIVE or INACTIVE). INACTIVE blocks the "
                    + "login and the token in use, but keeps the link, the seat and the enrollments. An employee "
                    + "without a password stays PENDING when set ACTIVE. Changing the e-mail of a PENDING "
                    + "employee invalidates the previous invite and e-mails a new one (inviteSent is null when "
                    + "no invite was issued). E-mail in use by another account returns 409; an employee of "
                    + "another company returns 404.")
    public ResponseEntity<ApiResponse<UpdatedEmployeeResponse>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyEmployeeRequest request
    ) {
        final UUID companyId = AuthenticationContext.requireUser().id();
        final UpdatedEmployee updated = updateCompanyEmployeeUseCase.execute(companyId, id, new EmployeeChanges(
                request.name(), request.email(), request.jobTitle(), request.status()));
        // Fora da transação da edição, como no cadastro: uma falha no envio não desfaz a edição.
        final Boolean inviteSent = updated.hasNewInvite()
                ? employeeInviteMailer.send(updated.employee().name(), updated.employee().email(),
                        updated.companyName(), updated.inviteToken())
                : null;
        return ResponseEntity.ok(ApiResponse.success(
                UpdatedEmployeeResponse.from(updated, inviteSent), "Colaborador atualizado com sucesso"));
    }

    @DeleteMapping("/employees/{id}")
    @Operation(
            summary = "Unlink an employee from the company",
            description = "Removes the link (freeing the seat), revokes the enrollments in the courses of the "
                    + "company, sets the account INACTIVE and invalidates pending invites, all in one "
                    + "transaction. Progress and certificates are kept, and courses the employee bought on "
                    + "their own are not revoked. Returns the updated seats summary; 404 when the employee "
                    + "does not belong to the company of the token.")
    public ResponseEntity<ApiResponse<CompanySeatsResponse>> unlinkEmployee(@PathVariable UUID id) {
        final UUID companyId = AuthenticationContext.requireUser().id();
        return ResponseEntity.ok(ApiResponse.success(
                CompanySeatsResponse.from(unlinkCompanyEmployeeUseCase.execute(companyId, id))));
    }

    @GetMapping("/employees/{id}/courses")
    @Operation(
            summary = "List the company courses released to an employee",
            description = "Returns every published course owned by the company with a flag saying whether the "
                    + "employee currently has an active enrollment. An employee of another company returns 404.")
    public ResponseEntity<ApiResponse<List<EmployeeCourseAssignmentResponse>>> listEmployeeCourses(
            @PathVariable UUID id
    ) {
        final UUID companyId = AuthenticationContext.requireUser().id();
        final List<EmployeeCourseAssignmentResponse> courses = listEmployeeCoursesUseCase.execute(companyId, id)
                .stream()
                .map(EmployeeCourseAssignmentResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @PostMapping("/employees/{id}/courses/{courseId}")
    @Operation(
            summary = "Release a company course to an employee",
            description = "Enrolls (or reactivates) the employee in a course owned by the company. A course the "
                    + "company does not own, or an employee of another company, returns 404.")
    public ResponseEntity<ApiResponse<Void>> assignEmployeeCourse(
            @PathVariable UUID id,
            @PathVariable UUID courseId
    ) {
        final UUID companyId = AuthenticationContext.requireUser().id();
        assignEmployeeCourseUseCase.execute(companyId, id, courseId);
        return ResponseEntity.ok(ApiResponse.success(null, "Curso liberado para o colaborador"));
    }

    @DeleteMapping("/employees/{id}/courses/{courseId}")
    @Operation(
            summary = "Revoke a company course from an employee",
            description = "Revokes the corporate enrollment. Courses the employee bought individually stay "
                    + "available. Progress and certificates are kept. Returns 404 for another company's "
                    + "employee or a course the company does not own.")
    public ResponseEntity<ApiResponse<Void>> revokeEmployeeCourse(
            @PathVariable UUID id,
            @PathVariable UUID courseId
    ) {
        final UUID companyId = AuthenticationContext.requireUser().id();
        revokeEmployeeCourseUseCase.execute(companyId, id, courseId);
        return ResponseEntity.ok(ApiResponse.success(null, "Acesso ao curso removido"));
    }
}
