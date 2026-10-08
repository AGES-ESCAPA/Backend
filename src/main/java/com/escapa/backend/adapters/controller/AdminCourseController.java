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

import com.escapa.backend.adapters.dto.AddCoursePrerequisiteRequest;
import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.ChangeLogPageResponse;
import com.escapa.backend.adapters.dto.CourseRulesResponse;
import com.escapa.backend.adapters.dto.CourseSearchResponse;
import com.escapa.backend.adapters.dto.PublishCourseRequest;
import com.escapa.backend.adapters.dto.UpdateProgressRulesRequest;
import com.escapa.backend.adapters.dto.course.AdminCourseListItemResponse;
import com.escapa.backend.adapters.dto.course.CourseResponse;
import com.escapa.backend.adapters.dto.course.CreateCourseRequest;
import com.escapa.backend.adapters.dto.course.UpdateCourseRequest;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.adapters.security.UserIdHeader;
import com.escapa.backend.application.usecase.AddCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.ArchiveCourseUseCase;
import com.escapa.backend.application.usecase.CreateCourseUseCase;
import com.escapa.backend.application.usecase.GetAdminCourseUseCase;
import com.escapa.backend.application.usecase.GetCourseChangeLogUseCase;
import com.escapa.backend.application.usecase.GetCourseRulesUseCase;
import com.escapa.backend.application.usecase.ListAdminCoursesUseCase;
import com.escapa.backend.application.usecase.ListCourseCategoriesUseCase;
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
    private final GetAdminCourseUseCase getAdminCourseUseCase;
    private final CreateCourseUseCase createCourseUseCase;
    private final PublishCourseUseCase publishCourseUseCase;
    private final UpdateCourseUseCase updateCourseUseCase;
    private final ListAdminCoursesUseCase listAdminCoursesUseCase;
    private final ListCourseCategoriesUseCase listCourseCategoriesUseCase;
    private final ArchiveCourseUseCase archiveCourseUseCase;
    private final AdminRequestGuard adminRequestGuard;

    public AdminCourseController(
            GetCourseRulesUseCase getCourseRulesUseCase,
            UpdateProgressRulesUseCase updateProgressRulesUseCase,
            AddCoursePrerequisiteUseCase addCoursePrerequisiteUseCase,
            RemoveCoursePrerequisiteUseCase removeCoursePrerequisiteUseCase,
            SearchCoursesForPrerequisiteUseCase searchCoursesForPrerequisiteUseCase,
            GetCourseChangeLogUseCase getCourseChangeLogUseCase,
            GetAdminCourseUseCase getAdminCourseUseCase,
            CreateCourseUseCase createCourseUseCase,
            PublishCourseUseCase publishCourseUseCase,
            UpdateCourseUseCase updateCourseUseCase,
            ListAdminCoursesUseCase listAdminCoursesUseCase,
            ListCourseCategoriesUseCase listCourseCategoriesUseCase,
            ArchiveCourseUseCase archiveCourseUseCase,
            AdminRequestGuard adminRequestGuard) {
        this.getCourseRulesUseCase = getCourseRulesUseCase;
        this.updateProgressRulesUseCase = updateProgressRulesUseCase;
        this.addCoursePrerequisiteUseCase = addCoursePrerequisiteUseCase;
        this.removeCoursePrerequisiteUseCase = removeCoursePrerequisiteUseCase;
        this.searchCoursesForPrerequisiteUseCase = searchCoursesForPrerequisiteUseCase;
        this.getCourseChangeLogUseCase = getCourseChangeLogUseCase;
        this.getAdminCourseUseCase = getAdminCourseUseCase;
        this.createCourseUseCase = createCourseUseCase;
        this.publishCourseUseCase = publishCourseUseCase;
        this.updateCourseUseCase = updateCourseUseCase;
        this.listAdminCoursesUseCase = listAdminCoursesUseCase;
        this.listCourseCategoriesUseCase = listCourseCategoriesUseCase;
        this.archiveCourseUseCase = archiveCourseUseCase;
        this.adminRequestGuard = adminRequestGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminCourseListItemResponse>>> list(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId) {
        adminRequestGuard.requireAdmin(xUserId);
        final List<AdminCourseListItemResponse> courses = listAdminCoursesUseCase.execute().stream()
                .map(AdminCourseListItemResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> listCategories(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId) {
        adminRequestGuard.requireAdmin(xUserId);
        return ResponseEntity.ok(ApiResponse.success(listCourseCategoriesUseCase.execute()));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> getById(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId) {
        adminRequestGuard.requireAdmin(xUserId);
        final Course course = getAdminCourseUseCase.execute(courseId);
        return ResponseEntity.ok(ApiResponse.success(CourseResponse.from(course)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @Valid @RequestBody CreateCourseRequest request) {
        final User admin = adminRequestGuard.requireAdmin(xUserId);
        final Course course = createCourseUseCase.execute(
                request.title(), request.shortDescription(), request.description(), request.thumbnailUrl(),
                request.teaserVideoUrl(), request.instructorId(), request.category(), request.level(),
                request.durationTime(), request.deadline(), request.accessDurationDays(), request.price(),
                request.learningObjectives(), request.requireSequentialProgress(), request.enforceDeadlineBlock(),
                admin.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(CourseResponse.from(course), "Course created successfully"));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Void>> archive(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId) {
        adminRequestGuard.requireAdmin(xUserId);
        archiveCourseUseCase.execute(courseId);
        return ResponseEntity.ok(ApiResponse.success(null, "Course archived successfully"));
    }

    @GetMapping("/{courseId}/rules")
    public CourseRulesResponse getRules(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId
    ) {
        adminRequestGuard.requireAdmin(xUserId);
        return CourseRulesResponse.from(getCourseRulesUseCase.execute(courseId));
    }

    @PutMapping("/{courseId}/progress-rules")
    public CourseRulesResponse updateProgressRules(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateProgressRulesRequest request
    ) {
        final User admin = adminRequestGuard.requireAdmin(xUserId);
        updateProgressRulesUseCase.execute(
                courseId, request.requireSequentialProgress(), request.enforceDeadlineBlock(), admin);
        return CourseRulesResponse.from(getCourseRulesUseCase.execute(courseId));
    }

    @PostMapping("/{courseId}/prerequisites")
    public void addPrerequisite(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId,
            @Valid @RequestBody AddCoursePrerequisiteRequest request
    ) {
        final User admin = adminRequestGuard.requireAdmin(xUserId);
        addCoursePrerequisiteUseCase.execute(courseId, request.prerequisiteCourseId(), admin);
    }

    @GetMapping("/{courseId}/prerequisites/search")
    public List<CourseSearchResponse> searchPrerequisites(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId,
            @RequestParam String query) {
        adminRequestGuard.requireAdmin(xUserId);
        return searchCoursesForPrerequisiteUseCase.execute(courseId, query).stream()
                .map(CourseSearchResponse::from)
                .toList();
    }

    @DeleteMapping("/{courseId}/prerequisites/{prerequisiteCourseId}")
    public void removePrerequisite(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId,
            @PathVariable UUID prerequisiteCourseId) {
        final User admin = adminRequestGuard.requireAdmin(xUserId);
        removeCoursePrerequisiteUseCase.execute(courseId, prerequisiteCourseId, admin);
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request) {
        final User admin = adminRequestGuard.requireAdmin(xUserId);
        final Course course = updateCourseUseCase.execute(
                courseId, request.title(), request.shortDescription(), request.description(), request.thumbnailUrl(),
                request.teaserVideoUrl(), request.instructorId(), request.category(), request.level(),
                request.durationTime(), request.deadline(), request.accessDurationDays(), request.price(),
                request.learningObjectives(), request.requireSequentialProgress(), request.enforceDeadlineBlock(),
                admin
        );
        return ResponseEntity.ok(ApiResponse.success(CourseResponse.from(course), "Course updated successfully"));
    }

    @PostMapping("/{courseId}/publish")
    public ResponseEntity<ApiResponse<CourseResponse>> publishCourse(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId,
            @RequestBody(required = false) PublishCourseRequest request) {
        final User admin = adminRequestGuard.requireAdmin(xUserId);
        final boolean notify = request != null && Boolean.TRUE.equals(request.notifyEnrolledStudents());
        final Course course = publishCourseUseCase.execute(courseId, notify, admin);
        return ResponseEntity.ok(ApiResponse.success(CourseResponse.from(course), "Course published successfully"));
    }

    @GetMapping("/{courseId}/change-log")
    public ChangeLogPageResponse getChangeLog(
            @RequestHeader(value = UserIdHeader.NAME, required = false) String xUserId,
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = PaginationDefaults.FIRST_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.CHANGE_LOG_PAGE_SIZE) int size) {
        adminRequestGuard.requireAdmin(xUserId);
        return ChangeLogPageResponse.from(getCourseChangeLogUseCase.execute(courseId, page, size));
    }
}
