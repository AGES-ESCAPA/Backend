package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.CourseRulesResponse;
import com.escapa.backend.application.usecase.GetCourseRulesUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/courses")
public class CourseController {
    private final GetCourseRulesUseCase getCourseRulesUseCase;

    public CourseController(GetCourseRulesUseCase getCourseRulesUseCase) {
        this.getCourseRulesUseCase = getCourseRulesUseCase;
    }

    @GetMapping("/{courseId}/rules")
    public CourseRulesResponse getCourseRules(@PathVariable UUID courseId) {
        return getCourseRulesUseCase.execute(courseId);
    }
}
