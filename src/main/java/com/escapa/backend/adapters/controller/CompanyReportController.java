package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CompanyEmployeeProgressResponse;
import com.escapa.backend.adapters.dto.CompanyReportOverviewResponse;
import com.escapa.backend.adapters.dto.PageResponse;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CompanyEmployeeProgress;
import com.escapa.backend.application.model.EmployeeProgressStatus;
import com.escapa.backend.application.usecase.GetCompanyReportOverviewUseCase;
import com.escapa.backend.application.usecase.ListCompanyEmployeeProgressUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Relatório de progresso da equipe da empresa autenticada (US-35). A empresa é sempre a do token
 * ({@code company.id} é o mesmo id do usuário); nenhuma rota recebe o id da empresa.
 */
@RestController
@Tag(name = "Company - Reports", description = "Team progress reports of the authenticated company")
@RequestMapping("/api/v1/company/reports")
public class CompanyReportController {

    private final GetCompanyReportOverviewUseCase getCompanyReportOverviewUseCase;
    private final ListCompanyEmployeeProgressUseCase listCompanyEmployeeProgressUseCase;

    public CompanyReportController(
            GetCompanyReportOverviewUseCase getCompanyReportOverviewUseCase,
            ListCompanyEmployeeProgressUseCase listCompanyEmployeeProgressUseCase
    ) {
        this.getCompanyReportOverviewUseCase = getCompanyReportOverviewUseCase;
        this.listCompanyEmployeeProgressUseCase = listCompanyEmployeeProgressUseCase;
    }

    @GetMapping("/overview")
    @Operation(
            summary = "Completion and engagement indicators of the team",
            description = "Considers only the non-revoked enrollments of the employees of the company in its "
                    + "courses. A company without employees gets zeroed indicators; monthlyHours always has "
                    + "6 months.")
    public ResponseEntity<ApiResponse<CompanyReportOverviewResponse>> getOverview() {
        final UUID companyId = AuthenticationContext.requireUser().id();
        return ResponseEntity.ok(ApiResponse.success(
                CompanyReportOverviewResponse.from(getCompanyReportOverviewUseCase.execute(companyId))));
    }

    @GetMapping("/employees")
    @Operation(
            summary = "Progress of each employee in each course of the company",
            description = "One row per employee and course, paginated and ordered by the most recent access "
                    + "(never accessed last). The optional search filters by employee name, ignoring case; "
                    + "status is IN_PROGRESS or COMPLETED.")
    public ResponseEntity<ApiResponse<PageResponse<CompanyEmployeeProgressResponse>>> listEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EmployeeProgressStatus status,
            @RequestParam(defaultValue = PaginationDefaults.FIRST_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.PAGE_SIZE) int size
    ) {
        final UUID companyId = AuthenticationContext.requireUser().id();
        final PageResult<CompanyEmployeeProgress> result =
                listCompanyEmployeeProgressUseCase.execute(companyId, search, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result, CompanyEmployeeProgressResponse::from)));
    }
}
