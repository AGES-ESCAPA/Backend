package com.escapa.backend.adapters.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    public AdminCourseController(
            GetCourseRulesUseCase getCourseRulesUseCase,
            UpdateProgressRulesUseCase updateProgressRulesUseCase,
            AddCoursePrerequisiteUseCase addCoursePrerequisiteUseCase,
            RemoveCoursePrerequisiteUseCase removeCoursePrerequisiteUseCase,
            SearchCoursesForPrerequisiteUseCase searchCoursesForPrerequisiteUseCase,
            GetCourseChangeLogUseCase getCourseChangeLogUseCase,
            PublishCourseUseCase publishCourseUseCase,
            UpdateCourseUseCase updateCourseUseCase) {
        this.getCourseRulesUseCase = getCourseRulesUseCase;
        this.updateProgressRulesUseCase = updateProgressRulesUseCase;
        this.addCoursePrerequisiteUseCase = addCoursePrerequisiteUseCase;
        this.removeCoursePrerequisiteUseCase = removeCoursePrerequisiteUseCase;
        this.searchCoursesForPrerequisiteUseCase = searchCoursesForPrerequisiteUseCase;
        this.getCourseChangeLogUseCase = getCourseChangeLogUseCase;
        this.publishCourseUseCase = publishCourseUseCase;
        this.updateCourseUseCase = updateCourseUseCase;
    }

    @GetMapping("/{courseId}/rules")
    public CourseRulesResponse getRules(
        @PathVariable UUID courseId
    ) {
        return getCourseRulesUseCase.execute(courseId);
    }

    @PutMapping("/{courseId}/progress-rules")
    public CourseRulesResponse updateProgressRules(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateProgressRulesRequest request
    ) {
        updateProgressRulesUseCase.execute(courseId, request);
        return getCourseRulesUseCase.execute(courseId);
    }
    @PostMapping("/{courseId}/prerequisites")
    public void addPrerequisite(
            @PathVariable UUID courseId,
            @Valid @RequestBody AddCoursePrerequisiteRequest request
    ) {
        addCoursePrerequisiteUseCase.execute(courseId, request);
    }

    @GetMapping("/{courseId}/prerequisites/search")
    public List<CourseSearchResponse> searchPrerequisites(
            @PathVariable UUID courseId,
            @RequestParam String query) {
        return searchCoursesForPrerequisiteUseCase.execute(courseId, query);
    }

    @DeleteMapping("/{courseId}/prerequisites/{prerequisiteCourseId}")
    public void removePrerequisite(
            @PathVariable UUID courseId,
            @PathVariable UUID prerequisiteCourseId) {
        removeCoursePrerequisiteUseCase.execute(courseId, prerequisiteCourseId, null);
    }

    @PutMapping("/{courseId}")
    public void updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request) {
        updateCourseUseCase.execute(courseId, request, null);
    }

    @PostMapping("/{courseId}/publish")
    public void publishCourse(
            @PathVariable UUID courseId,
            @RequestBody(required = false) PublishCourseRequest request) {
        final PublishCourseRequest publishRequest = request == null
                ? new PublishCourseRequest(false) : request;
        publishCourseUseCase.execute(courseId, publishRequest, null);
    }

    @GetMapping("/{courseId}/change-log")
    public ChangeLogPageResponse getChangeLog(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return getCourseChangeLogUseCase.execute(courseId, page, size);
    }
}
