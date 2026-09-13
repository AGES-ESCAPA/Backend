package com.escapa.backend.adapters.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.escapa.backend.adapters.dto.AddCoursePrerequisiteRequest;
import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.ChangeLogPageResponse;
import com.escapa.backend.adapters.dto.CourseRulesResponse;
import com.escapa.backend.adapters.dto.CourseSearchResponse;
import com.escapa.backend.adapters.dto.PublishCourseRequest;
import com.escapa.backend.adapters.dto.UpdateCourseRequest;
import com.escapa.backend.adapters.dto.UpdateProgressRulesRequest;
import com.escapa.backend.adapters.dto.course.AdminCourseListItemResponse;
import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CourseRules;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.AddCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.ArchiveCourseUseCase;
import com.escapa.backend.application.usecase.GetCourseChangeLogUseCase;
import com.escapa.backend.application.usecase.GetCourseRulesUseCase;
import com.escapa.backend.application.usecase.ListAdminCoursesUseCase;
import com.escapa.backend.application.usecase.PublishCourseUseCase;
import com.escapa.backend.application.usecase.RemoveCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.SearchCoursesForPrerequisiteUseCase;
import com.escapa.backend.application.usecase.UpdateCourseUseCase;
import com.escapa.backend.application.usecase.UpdateProgressRulesUseCase;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/courses")
public class AdminCourseController {
    private final GetCourseRulesUseCase getCourseRulesUseCase;
    private final UpdateProgressRulesUseCase updateProgressRulesUseCase;
    private final AddCoursePrerequisiteUseCase addCoursePrerequisiteUseCase;
    private final RemoveCoursePrerequisiteUseCase removeCoursePrerequisiteUseCase;
    private final SearchCoursesForPrerequisiteUseCase searchCoursesForPrerequisiteUseCase;
    private final GetCourseChangeLogUseCase getCourseChangeLogUseCase;
    private final PublishCourseUseCase publishCourseUseCase;
    private final UpdateCourseUseCase updateCourseUseCase;
    private final ListAdminCoursesUseCase listAdminCoursesUseCase;
    private final ArchiveCourseUseCase archiveCourseUseCase;
    private final UserRepositoryPort userRepositoryPort;

    public AdminCourseController(
            GetCourseRulesUseCase getCourseRulesUseCase,
            UpdateProgressRulesUseCase updateProgressRulesUseCase,
            AddCoursePrerequisiteUseCase addCoursePrerequisiteUseCase,
            RemoveCoursePrerequisiteUseCase removeCoursePrerequisiteUseCase,
            SearchCoursesForPrerequisiteUseCase searchCoursesForPrerequisiteUseCase,
            GetCourseChangeLogUseCase getCourseChangeLogUseCase,
            PublishCourseUseCase publishCourseUseCase,
            UpdateCourseUseCase updateCourseUseCase,
            ListAdminCoursesUseCase listAdminCoursesUseCase,
            ArchiveCourseUseCase archiveCourseUseCase,
            UserRepositoryPort userRepositoryPort) {
        this.getCourseRulesUseCase = getCourseRulesUseCase;
        this.updateProgressRulesUseCase = updateProgressRulesUseCase;
        this.addCoursePrerequisiteUseCase = addCoursePrerequisiteUseCase;
        this.removeCoursePrerequisiteUseCase = removeCoursePrerequisiteUseCase;
        this.searchCoursesForPrerequisiteUseCase = searchCoursesForPrerequisiteUseCase;
        this.getCourseChangeLogUseCase = getCourseChangeLogUseCase;
        this.publishCourseUseCase = publishCourseUseCase;
        this.updateCourseUseCase = updateCourseUseCase;
        this.listAdminCoursesUseCase = listAdminCoursesUseCase;
        this.archiveCourseUseCase = archiveCourseUseCase;
        this.userRepositoryPort = userRepositoryPort;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminCourseListItemResponse>>> list(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId) {
        requireAdmin(xUserId);
        final List<AdminCourseListItemResponse> courses = listAdminCoursesUseCase.execute().stream()
                .map(AdminCourseController::toListItem)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> archive(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId) {
        requireAdmin(xUserId);
        archiveCourseUseCase.execute(courseId);
        return ResponseEntity.ok(ApiResponse.success(null, "Course archived successfully"));
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

    @GetMapping("/{courseId}/rules")
    public CourseRulesResponse getRules(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId
    ) {
        requireAdmin(xUserId);
        return toResponse(getCourseRulesUseCase.execute(courseId));
    }

    @PutMapping("/{courseId}/progress-rules")
    public CourseRulesResponse updateProgressRules(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateProgressRulesRequest request
    ) {
        final User admin = requireAdmin(xUserId);
        updateProgressRulesUseCase.execute(
                courseId, request.requireSequentialProgress(), request.enforceDeadlineBlock(), admin);
        return toResponse(getCourseRulesUseCase.execute(courseId));
    }

    @PostMapping("/{courseId}/prerequisites")
    public void addPrerequisite(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId,
            @Valid @RequestBody AddCoursePrerequisiteRequest request
    ) {
        final User admin = requireAdmin(xUserId);
        addCoursePrerequisiteUseCase.execute(courseId, request.prerequisiteCourseId(), admin);
    }

    @GetMapping("/{courseId}/prerequisites/search")
    public List<CourseSearchResponse> searchPrerequisites(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId,
            @RequestParam String query) {
        requireAdmin(xUserId);
        return searchCoursesForPrerequisiteUseCase.execute(courseId, query).stream()
                .map(course -> new CourseSearchResponse(course.getId(), course.getTitle()))
                .toList();
    }

    @DeleteMapping("/{courseId}/prerequisites/{prerequisiteCourseId}")
    public void removePrerequisite(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId,
            @PathVariable UUID prerequisiteCourseId) {
        final User admin = requireAdmin(xUserId);
        removeCoursePrerequisiteUseCase.execute(courseId, prerequisiteCourseId, admin);
    }

    @PutMapping("/{courseId}")
    public void updateCourse(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request) {
        final User admin = requireAdmin(xUserId);
        updateCourseUseCase.execute(
                courseId, request.title(), null, request.description(), null, null, null, null, null,
                null, null, null, null, null, null, null, admin);
    }

    @PostMapping("/{courseId}/publish")
    public void publishCourse(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId,
            @RequestBody(required = false) PublishCourseRequest request) {
        final User admin = requireAdmin(xUserId);
        final boolean notify = request != null && Boolean.TRUE.equals(request.notifyEnrolledStudents());
        publishCourseUseCase.execute(courseId, notify, admin);
    }

    @GetMapping("/{courseId}/change-log")
    public ChangeLogPageResponse getChangeLog(
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(xUserId);
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
        final PageResult<ChangeLogEntry> result = getCourseChangeLogUseCase.execute(courseId, page, size);
        return new ChangeLogPageResponse(
                result.content().stream().map(AdminCourseController::toEntry).toList(),
                result.pageNumber(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    private static ChangeLogPageResponse.Entry toEntry(ChangeLogEntry entry) {
        return new ChangeLogPageResponse.Entry(
                entry.id(), entry.description(), entry.changedByName(),
                entry.majorVersion() + "." + entry.minorVersion(),
                entry.createdAt() == null ? null : entry.createdAt().toString());
    }

    private static AdminCourseListItemResponse toListItem(Course course) {
        final int major = course.getMajorVersion() == null ? 0 : course.getMajorVersion();
        final int minor = course.getMinorVersion() == null ? 0 : course.getMinorVersion();
        return new AdminCourseListItemResponse(
                course.getId(),
                course.getTitle(),
                course.getCategory(),
                course.getPrice(),
                course.getStatus(),
                major,
                minor);
    }

    private static CourseRulesResponse toResponse(CourseRules rules) {
        return new CourseRulesResponse(
                rules.requireSequentialProgress(),
                rules.enforceDeadlineBlock(),
                rules.version(),
                rules.prerequisites().stream()
                        .map(p -> new CourseRulesResponse.PrerequisiteResponse(
                                p.courseId().toString(), p.courseTitle()))
                        .toList(),
                rules.recentChangeLog().stream()
                        .map(log -> new CourseRulesResponse.ChangeLogResponse(
                                log.id(), log.description(), log.changedByName(),
                                log.createdAt() == null ? null : log.createdAt().toString()))
                        .toList());
    }
}
