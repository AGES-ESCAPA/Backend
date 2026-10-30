package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CompanyEmployeeResponse;
import com.escapa.backend.adapters.dto.CompanySeatsResponse;
import com.escapa.backend.adapters.dto.PageResponse;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployee;
import com.escapa.backend.application.usecase.GetCompanySeatsSummaryUseCase;
import com.escapa.backend.application.usecase.ListCompanyEmployeesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    public CompanyEmployeeController(
            GetCompanySeatsSummaryUseCase getCompanySeatsSummaryUseCase,
            ListCompanyEmployeesUseCase listCompanyEmployeesUseCase
    ) {
        this.getCompanySeatsSummaryUseCase = getCompanySeatsSummaryUseCase;
        this.listCompanyEmployeesUseCase = listCompanyEmployeesUseCase;
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
}
