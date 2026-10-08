package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.PageResponse;
import com.escapa.backend.adapters.dto.StudentCourseCardResponse;
import com.escapa.backend.adapters.security.UserIdHeader;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.application.usecase.ListStudentEnrollmentsUseCase;
import com.escapa.backend.domain.course.EnrollmentStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/student")
public class StudentEnrollmentController {

    private final ListStudentEnrollmentsUseCase listStudentEnrollmentsUseCase;

    public StudentEnrollmentController(ListStudentEnrollmentsUseCase listStudentEnrollmentsUseCase) {
        this.listStudentEnrollmentsUseCase = listStudentEnrollmentsUseCase;
    }

    @GetMapping("/enrollments")
    public ResponseEntity<ApiResponse<PageResponse<StudentCourseCardResponse>>> listEnrollments(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(defaultValue = PaginationDefaults.FIRST_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.PAGE_SIZE) int size
    ) {
        final UUID userId = UserIdHeader.requireStudentId(xUserId);
        final PageResult<StudentCourseCard> result = listStudentEnrollmentsUseCase.execute(userId, query, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(result, StudentCourseCardResponse::from)));
    }
}
