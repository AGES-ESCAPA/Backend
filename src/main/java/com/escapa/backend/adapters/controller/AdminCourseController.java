package com.escapa.backend.adapters.controller;

public class AdminCourseController {
    @GetMapping("/api/admin/courses/{courseId}/rules")
    public CourseRulesResponse getRules(
        @PathVariable UUID courseId
    ) {
        return getCourseRulesUseCase.execute(courseId);
    }

    @PutMapping("/api/admin/courses/{courseId}/progress-rules")
    public CourseRulesResponse updateProgressRules(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateProgressRulesRequest request
    ) {
        return updateProgressRulesUseCase.execute(courseId, request);
    }
    @PostMapping("/api/admin/courses/{courseId}/prerequisites")
    public void addPrerequisite(
            @PathVariable UUID courseId,
            @Valid @RequestBody AddCoursePrerequisiteRequest request
    ) {
        addCoursePrerequisiteUseCase.execute(courseId, request);
    }
}
