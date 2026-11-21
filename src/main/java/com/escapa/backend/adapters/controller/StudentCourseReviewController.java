package com.escapa.backend.adapters.controller;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CourseReviewRequest;
import com.escapa.backend.adapters.dto.CourseReviewResponse;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.RateCourseUseCase;

@RestController
@Tag(name = "Student - Course reviews", description = "Ratings the student gives to completed courses")
@RequestMapping("/api/v1/student")
public class StudentCourseReviewController {

    private final RateCourseUseCase rateCourseUseCase;

    public StudentCourseReviewController(RateCourseUseCase rateCourseUseCase) {
        this.rateCourseUseCase = rateCourseUseCase;
    }

    @PutMapping("/courses/{courseId}/review")
    @Operation(
            summary = "Rate a completed course",
            description = "Saves the 1 to 5 stars rating of the student; rating again replaces the previous one."
    )
    public ResponseEntity<ApiResponse<CourseReviewResponse>> rateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseReviewRequest request
    ) {
        final UUID userId = AuthenticationContext.requireUser().id();
        final int rating = rateCourseUseCase.execute(userId, courseId, request.rating());
        return ResponseEntity.ok(ApiResponse.success(new CourseReviewResponse(courseId, rating)));
    }
}
