package com.escapa.backend.adapters.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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
import com.escapa.backend.adapters.dto.ChangeLogPageResponse;
import com.escapa.backend.adapters.dto.CourseRulesResponse;
import com.escapa.backend.adapters.dto.CourseSearchResponse;
import com.escapa.backend.adapters.dto.PublishCourseRequest;
import com.escapa.backend.adapters.dto.UpdateCourseRequest;
import com.escapa.backend.adapters.dto.UpdateProgressRulesRequest;
import com.escapa.backend.application.usecase.AddCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.GetCourseChangeLogUseCase;
import com.escapa.backend.application.usecase.GetCourseRulesUseCase;
import com.escapa.backend.application.usecase.PublishCourseUseCase;
import com.escapa.backend.application.usecase.RemoveCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.SearchCoursesForPrerequisiteUseCase;
import com.escapa.backend.application.usecase.UpdateCourseUseCase;
import com.escapa.backend.application.usecase.UpdateProgressRulesUseCase;
import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/courses")
@SecurityRequirement(name = "bearerAuth")
public class AdminCourseController {
    private final GetCourseRulesUseCase getCourseRulesUseCase;
    private final UpdateProgressRulesUseCase updateProgressRulesUseCase;
    private final AddCoursePrerequisiteUseCase addCoursePrerequisiteUseCase;
    private final RemoveCoursePrerequisiteUseCase removeCoursePrerequisiteUseCase;
    private final SearchCoursesForPrerequisiteUseCase searchCoursesForPrerequisiteUseCase;
    private final GetCourseChangeLogUseCase getCourseChangeLogUseCase;
    private final UserJpaRepository userRepository;
    private final PublishCourseUseCase publishCourseUseCase;
    private final UpdateCourseUseCase updateCourseUseCase;

    public AdminCourseController(
            GetCourseRulesUseCase getCourseRulesUseCase,
            UpdateProgressRulesUseCase updateProgressRulesUseCase,
            AddCoursePrerequisiteUseCase addCoursePrerequisiteUseCase,
            RemoveCoursePrerequisiteUseCase removeCoursePrerequisiteUseCase,
            SearchCoursesForPrerequisiteUseCase searchCoursesForPrerequisiteUseCase,
            GetCourseChangeLogUseCase getCourseChangeLogUseCase,
            UserJpaRepository userRepository,
            PublishCourseUseCase publishCourseUseCase,
            UpdateCourseUseCase updateCourseUseCase) {
        this.getCourseRulesUseCase = getCourseRulesUseCase;
        this.updateProgressRulesUseCase = updateProgressRulesUseCase;
        this.addCoursePrerequisiteUseCase = addCoursePrerequisiteUseCase;
        this.removeCoursePrerequisiteUseCase = removeCoursePrerequisiteUseCase;
        this.searchCoursesForPrerequisiteUseCase = searchCoursesForPrerequisiteUseCase;
        this.getCourseChangeLogUseCase = getCourseChangeLogUseCase;
        this.userRepository = userRepository;
        this.publishCourseUseCase = publishCourseUseCase;
        this.updateCourseUseCase = updateCourseUseCase;
    }

    @GetMapping("/{courseId}/rules")
    public CourseRulesResponse getRules(
        @PathVariable UUID courseId
            , @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);
        return getCourseRulesUseCase.execute(courseId);
    }

    @PutMapping("/{courseId}/progress-rules")
    public CourseRulesResponse updateProgressRules(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateProgressRulesRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        updateProgressRulesUseCase.execute(courseId, request, requireAdmin(authorization));
        return getCourseRulesUseCase.execute(courseId);
    }
    @PostMapping("/{courseId}/prerequisites")
    public void addPrerequisite(
            @PathVariable UUID courseId,
            @Valid @RequestBody AddCoursePrerequisiteRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        addCoursePrerequisiteUseCase.execute(courseId, request, requireAdmin(authorization));
    }

    @GetMapping("/{courseId}/prerequisites/search")
    public List<CourseSearchResponse> searchPrerequisites(
            @PathVariable UUID courseId,
            @RequestParam String query,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        return searchCoursesForPrerequisiteUseCase.execute(courseId, query);
    }

    @DeleteMapping("/{courseId}/prerequisites/{prerequisiteCourseId}")
    public void removePrerequisite(
            @PathVariable UUID courseId,
            @PathVariable UUID prerequisiteCourseId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        removeCoursePrerequisiteUseCase.execute(
                courseId, prerequisiteCourseId, requireAdmin(authorization));
    }

    @PutMapping("/{courseId}")
    public void updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        updateCourseUseCase.execute(courseId, request, requireAdmin(authorization));
    }

    @PostMapping("/{courseId}/publish")
    public void publishCourse(
            @PathVariable UUID courseId,
            @RequestBody(required = false) PublishCourseRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        final PublishCourseRequest publishRequest = request == null
                ? new PublishCourseRequest(false) : request;
        publishCourseUseCase.execute(courseId, publishRequest, requireAdmin(authorization));
    }

    @GetMapping("/{courseId}/change-log")
    public ChangeLogPageResponse getChangeLog(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdmin(authorization);
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
        return getCourseChangeLogUseCase.execute(courseId, page, size);
    }

    private UserEntity requireAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            final UUID userId = UUID.fromString(authorization.substring(7));
            final UserEntity user = userRepository.findById(userId).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN role required");
            }
            return user;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token", exception);
        }
    }
}
