package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CompanyCourseResponse;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.ListCompanyCourseIdsUseCase;
import com.escapa.backend.application.usecase.ListCompanyCoursesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Company - Courses", description = "Courses purchased by the authenticated company")
@RequestMapping("/api/v1/company")
public class CompanyCourseController {

    private final ListCompanyCourseIdsUseCase listCompanyCourseIdsUseCase;
    private final ListCompanyCoursesUseCase listCompanyCoursesUseCase;

    public CompanyCourseController(ListCompanyCourseIdsUseCase listCompanyCourseIdsUseCase,
                                    ListCompanyCoursesUseCase listCompanyCoursesUseCase
    ) {
        this.listCompanyCourseIdsUseCase = listCompanyCourseIdsUseCase;
        this.listCompanyCoursesUseCase = listCompanyCoursesUseCase;
    }

    @GetMapping("/courses")
    @Operation(summary = "List the active courses purchased by the company")
    public ResponseEntity<ApiResponse<List<CompanyCourseResponse>>> listCourses() {
        final UUID companyId = AuthenticationContext.requireUser().id();
        final List<CompanyCourseResponse> courses = listCompanyCoursesUseCase.execute(companyId).stream().map(CompanyCourseResponse::from).toList();

        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @GetMapping("/courses/acquired")
    @Operation(
            summary = "List the ids of the courses purchased by the company",
            description = "Includes active and expired purchases; a purchase revoked by order cancellation is left out.")
    public ResponseEntity<ApiResponse<List<UUID>>> listAcquiredCourseIds() {
        final UUID companyId = AuthenticationContext.requireUser().id();
        return ResponseEntity.ok(ApiResponse.success(listCompanyCourseIdsUseCase.execute(companyId)));
    }
}
