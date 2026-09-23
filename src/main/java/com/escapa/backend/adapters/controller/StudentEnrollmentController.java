package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.StudentCourseCardResponse;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.application.usecase.ListStudentEnrollmentsUseCase;
import com.escapa.backend.domain.course.EnrollmentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/student")
public class StudentEnrollmentController {

    private final ListStudentEnrollmentsUseCase listStudentEnrollmentsUseCase;

    public StudentEnrollmentController(ListStudentEnrollmentsUseCase listStudentEnrollmentsUseCase) {
        this.listStudentEnrollmentsUseCase = listStudentEnrollmentsUseCase;
    }

    @GetMapping("/enrollments")
    public ResponseEntity<ApiResponse<PageResult<StudentCourseCardResponse>>> listEnrollments(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        final UUID userId = requireUserId(xUserId);
        
        final PageResult<StudentCourseCard> result = listStudentEnrollmentsUseCase.execute(userId, query, status, page, size);
        
        final List<StudentCourseCardResponse> responseContent = result.content().stream()
                .map(StudentCourseCardResponse::from)
                .collect(Collectors.toList());

        final PageResult<StudentCourseCardResponse> responsePage = new PageResult<>(
                responseContent,
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages()
        );

        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    private UUID requireUserId(String xUserId) {
        if (xUserId == null || xUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        try {
            return UUID.fromString(xUserId.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid X-User-Id: must be a valid UUID");
        }
    }
}

