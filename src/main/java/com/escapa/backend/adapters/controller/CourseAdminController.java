package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.course.CourseResponse;
import com.escapa.backend.adapters.dto.course.CreateCourseRequest;
import com.escapa.backend.adapters.dto.course.UpdateCourseRequest;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.ArchiveCourseUseCase;
import com.escapa.backend.application.usecase.CreateCourseUseCase;
import com.escapa.backend.application.usecase.PublishCourseUseCase;
import com.escapa.backend.application.usecase.UpdateCourseUseCase;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/courses")
public class CourseAdminController {

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

    private User requireAdmin(String xUserId) {
        if (xUserId == null || xUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing X-User-Id header");
        }
        try {
            final UUID userId = UUID.fromString(xUserId);
            final User user = userRepositoryPort.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
            if (!"ADMIN".equalsIgnoreCase(user.getUserType())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: user is not ADMIN");
            }
            return user;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid X-User-Id: must be a valid UUID");
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @Valid @RequestBody CreateCourseRequest request) {

        final User admin = requireAdmin(xUserId);

        final Course course = createCourseUseCase.execute(
                request.title(), request.shortDescription(), request.description(), request.thumbnailUrl(),
                request.teaserVideoUrl(), request.instructorId(), request.category(), request.level(),
                request.durationTime(), request.deadline(), request.accessDurationDays(), request.price(),
                request.learningObjectives(), request.requireSequentialProgress(), request.enforceDeadlineBlock(),
                admin.getId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(course), "Course created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> update(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID id,
            @RequestBody UpdateCourseRequest request) {

        final User admin = requireAdmin(xUserId);

        final Course course = updateCourseUseCase.execute(
                id, request.title(), request.shortDescription(), request.description(), request.thumbnailUrl(),
                request.teaserVideoUrl(), request.instructorId(), request.category(), request.level(),
                request.durationTime(), request.deadline(), request.accessDurationDays(), request.price(),
                request.learningObjectives(), request.requireSequentialProgress(), request.enforceDeadlineBlock(),
                admin
        );

        return ResponseEntity.ok(ApiResponse.success(toResponse(course), "Course updated successfully"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<CourseResponse>> publish(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID id) {

        final User admin = requireAdmin(xUserId);

        final Course course = publishCourseUseCase.execute(id, false, admin);
        return ResponseEntity.ok(ApiResponse.success(toResponse(course), "Course published successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID id) {

        requireAdmin(xUserId);

        archiveCourseUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Course archived successfully"));
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
