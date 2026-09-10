package com.escapa.backend.adapters.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapa.backend.adapters.dto.AddCoursePrerequisiteRequest;
import com.escapa.backend.adapters.dto.CourseRulesResponse;
import com.escapa.backend.adapters.dto.UpdateProgressRulesRequest;
import com.escapa.backend.application.usecase.AddCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.GetCourseRulesUseCase;
import com.escapa.backend.application.usecase.UpdateProgressRulesUseCase;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/courses")
public class AdminCourseController {
    private final GetCourseRulesUseCase getCourseRulesUseCase;
    private final UpdateProgressRulesUseCase updateProgressRulesUseCase;
    private final AddCoursePrerequisiteUseCase addCoursePrerequisiteUseCase;

    public AdminCourseController(
            GetCourseRulesUseCase getCourseRulesUseCase,
            UpdateProgressRulesUseCase updateProgressRulesUseCase,
            AddCoursePrerequisiteUseCase addCoursePrerequisiteUseCase) {
        this.getCourseRulesUseCase = getCourseRulesUseCase;
        this.updateProgressRulesUseCase = updateProgressRulesUseCase;
        this.addCoursePrerequisiteUseCase = addCoursePrerequisiteUseCase;
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
}
