package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.ListCompanyCourseIdsUseCase;
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

    public CompanyCourseController(ListCompanyCourseIdsUseCase listCompanyCourseIdsUseCase) {
        this.listCompanyCourseIdsUseCase = listCompanyCourseIdsUseCase;
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
