package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.CourseCardResponse;
import com.escapa.backend.adapters.dto.PageResponse;
import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.usecase.ListPublishedCoursesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/courses")
public class PublicCourseController {

    private final ListPublishedCoursesUseCase listPublishedCoursesUseCase;

    public PublicCourseController(ListPublishedCoursesUseCase listPublishedCoursesUseCase) {
        this.listPublishedCoursesUseCase = listPublishedCoursesUseCase;
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
        final List<CourseCardResponse> cards = result.content().stream()
                .map(PublicCourseController::toResponse)
                .toList();
        return ResponseEntity.ok(new PageResponse<>(
                cards, result.pageNumber(), result.pageSize(),
                result.totalElements(), result.totalPages()));
    }

    private static CourseCardResponse toResponse(CourseSummary summary) {
        return new CourseCardResponse(
                summary.id(), summary.title(), summary.shortDescription(),
                summary.category(), summary.level(), summary.durationTime(),
                summary.lessonsCount(), summary.price(), summary.thumbnailUrl(),
                summary.instructorName(), summary.ratingAverage(), summary.reviewsCount());
    }
}
