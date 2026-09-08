package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.course.CourseResponse;
import com.escapa.backend.adapters.dto.course.CreateCourseRequest;
import com.escapa.backend.adapters.dto.course.UpdateCourseRequest;
import com.escapa.backend.application.usecase.ArchiveCourseUseCase;
import com.escapa.backend.application.usecase.CreateCourseUseCase;
import com.escapa.backend.application.usecase.PublishCourseUseCase;
import com.escapa.backend.application.usecase.UpdateCourseUseCase;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.application.port.UserRepositoryPort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/courses")
public class CourseAdminController {

    private static final int BEARER_PREFIX_LENGTH = 7;

    private final CreateCourseUseCase createCourseUseCase;
    private final UpdateCourseUseCase updateCourseUseCase;
    private final PublishCourseUseCase publishCourseUseCase;
    private final ArchiveCourseUseCase archiveCourseUseCase;
    private final UserRepositoryPort userRepositoryPort;

    public CourseAdminController(CreateCourseUseCase createCourseUseCase,
                                 UpdateCourseUseCase updateCourseUseCase,
                                 PublishCourseUseCase publishCourseUseCase,
                                 ArchiveCourseUseCase archiveCourseUseCase,
                                 UserRepositoryPort userRepositoryPort) {
        this.createCourseUseCase = createCourseUseCase;
        this.updateCourseUseCase = updateCourseUseCase;
        this.publishCourseUseCase = publishCourseUseCase;
        this.archiveCourseUseCase = archiveCourseUseCase;
        this.userRepositoryPort = userRepositoryPort;
    }

    private void requireAdmin(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Missing auth token");
        }
        try {
            final User user = userRepositoryPort.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
            if (!"ADMIN".equalsIgnoreCase(user.getUserType())) {
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "User is not ADMIN");
            }
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token");
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateCourseRequest request) {
        
        final String userId = extractToken(authHeader);
        requireAdmin(userId);

        final Course course = createCourseUseCase.execute(
                request.title(), request.shortDescription(), request.description(), request.thumbnailUrl(),
                request.teaserVideoUrl(), request.instructorId(), request.category(), request.level(),
                request.durationTime(), request.deadline(), request.accessDurationDays(), request.price(),
                request.learningObjectives(), request.requireSequentialProgress(), request.enforceDeadlineBlock(),
                UUID.fromString(userId)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(course), "Course created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> update(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id,
            @RequestBody UpdateCourseRequest request) {

        final String userId = extractToken(authHeader);
        requireAdmin(userId);

        final Course course = updateCourseUseCase.execute(
                id, request.title(), request.shortDescription(), request.description(), request.thumbnailUrl(),
                request.teaserVideoUrl(), request.instructorId(), request.category(), request.level(),
                request.durationTime(), request.deadline(), request.accessDurationDays(), request.price(),
                request.learningObjectives(), request.requireSequentialProgress(), request.enforceDeadlineBlock()
        );

        return ResponseEntity.ok(ApiResponse.success(toResponse(course), "Course updated successfully"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<CourseResponse>> publish(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id) {

        final String userId = extractToken(authHeader);
        requireAdmin(userId);

        final Course course = publishCourseUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(toResponse(course), "Course published successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id) {

        final String userId = extractToken(authHeader);
        requireAdmin(userId);

        archiveCourseUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Course archived successfully"));
    }

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(BEARER_PREFIX_LENGTH);
        }
        return authHeader; // fallback if passed directly or empty
    }

    private CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
                course.getThumbnailUrl(),
                course.getTeaserVideoUrl(),
                course.getStatus(),
                course.getInstructor() != null ? course.getInstructor().getId() : null,
                course.getCreatedBy() != null ? course.getCreatedBy().getId() : null,
                course.getCategory(),
                course.getLevel(),
                course.getDurationTime(),
                course.getDeadline(),
                course.getAccessDurationDays(),
                course.getPrice(),
                course.getLearningObjectives(),
                course.getRequireSequentialProgress(),
                course.getEnforceDeadlineBlock(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}

