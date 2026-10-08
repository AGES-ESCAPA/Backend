package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CourseCardResponse;
import com.escapa.backend.adapters.dto.CourseDetailsResponse;
import com.escapa.backend.adapters.dto.CourseFiltersResponse;
import com.escapa.backend.adapters.dto.PageResponse;
import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.usecase.GetCourseDetailsUseCase;
import com.escapa.backend.application.usecase.ListPublishedCourseFiltersUseCase;
import com.escapa.backend.application.usecase.ListPublishedCoursesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/courses")
public class PublicCourseController {

    private final ListPublishedCoursesUseCase listPublishedCoursesUseCase;
    private final ListPublishedCourseFiltersUseCase listPublishedCourseFiltersUseCase;
    private final GetCourseDetailsUseCase getCourseDetailsUseCase;

    public PublicCourseController(
            ListPublishedCoursesUseCase listPublishedCoursesUseCase,
            ListPublishedCourseFiltersUseCase listPublishedCourseFiltersUseCase,
            GetCourseDetailsUseCase getCourseDetailsUseCase
    ) {
        this.listPublishedCoursesUseCase = listPublishedCoursesUseCase;
        this.listPublishedCourseFiltersUseCase = listPublishedCourseFiltersUseCase;
        this.getCourseDetailsUseCase = getCourseDetailsUseCase;
    }

    @GetMapping("/filters")
    public ResponseEntity<CourseFiltersResponse> filters() {
        return ResponseEntity.ok(CourseFiltersResponse.from(listPublishedCourseFiltersUseCase.execute()));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CourseCardResponse>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        final PageResult<CourseSummary> result = listPublishedCoursesUseCase.execute(
                title, category, level, page, size);
        return ResponseEntity.ok(PageResponse.of(result, CourseCardResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDetailsResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(CourseDetailsResponse.from(getCourseDetailsUseCase.execute(id))));
    }
}
