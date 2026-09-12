package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CourseCardResponse;
import com.escapa.backend.adapters.dto.CourseDetailsResponse;
import com.escapa.backend.adapters.dto.PageResponse;
import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.usecase.GetCourseDetailsUseCase;
import com.escapa.backend.application.usecase.ListPublishedCoursesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/courses")
public class PublicCourseController {

    private final ListPublishedCoursesUseCase listPublishedCoursesUseCase;
    private final GetCourseDetailsUseCase getCourseDetailsUseCase;

    public PublicCourseController(
            ListPublishedCoursesUseCase listPublishedCoursesUseCase,
            GetCourseDetailsUseCase getCourseDetailsUseCase
    ) {
        this.listPublishedCoursesUseCase = listPublishedCoursesUseCase;
        this.getCourseDetailsUseCase = getCourseDetailsUseCase;
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
                .map(PublicCourseController::toCardResponse)
                .toList();
        return ResponseEntity.ok(new PageResponse<>(
                cards, result.pageNumber(), result.pageSize(),
                result.totalElements(), result.totalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDetailsResponse>> getById(
            @PathVariable UUID id
    ) {
        final CourseDetails course = getCourseDetailsUseCase.execute(id);

        return ResponseEntity.ok(
                ApiResponse.success(toDetailsResponse(course))
        );
    }

    private static CourseCardResponse toCardResponse(CourseSummary summary) {
        return new CourseCardResponse(
                summary.id(), summary.title(), summary.shortDescription(),
                summary.category(), summary.level(), summary.durationTime(),
                summary.lessonsCount(), summary.price(), summary.thumbnailUrl(),
                summary.instructorName(), summary.ratingAverage(), summary.reviewsCount());
    }

    private static CourseDetailsResponse toDetailsResponse(CourseDetails course) {
        return new CourseDetailsResponse(
                course.id(),
                course.title(),
                course.shortDescription(),
                course.description(),
                course.category(),
                course.level(),
                course.durationTime(),
                course.price(),
                course.deadline(),
                course.thumbnailUrl(),
                course.rating(),
                course.reviewsCount(),
                course.studentsCount(),
                toInstructorResponse(course.instructor()),
                course.learningObjectives(),
                course.materials().stream()
                        .map(PublicCourseController::toMaterialResponse)
                        .toList(),
                course.modules().stream()
                        .map(PublicCourseController::toModuleResponse)
                        .toList()
        );
    }

    private static CourseDetailsResponse.InstructorResponse toInstructorResponse(
            CourseDetails.Instructor instructor
    ) {
        if (instructor == null) {
            return null;
        }

        return new CourseDetailsResponse.InstructorResponse(
                instructor.id(),
                instructor.name(),
                instructor.headline(),
                instructor.bio()
        );
    }

    private static CourseDetailsResponse.MaterialResponse toMaterialResponse(
            CourseDetails.Material material
    ) {
        return new CourseDetailsResponse.MaterialResponse(
                material.title(),
                material.format(),
                material.fileUrl()
        );
    }

    private static CourseDetailsResponse.ModuleResponse toModuleResponse(
            CourseDetails.Module module
    ) {
        return new CourseDetailsResponse.ModuleResponse(
                module.id(),
                module.title(),
                module.order(),
                module.totalContents(),
                module.durationMinutes(),
                module.contents().stream()
                        .map(PublicCourseController::toContentResponse)
                        .toList()
        );
    }

    private static CourseDetailsResponse.ContentResponse toContentResponse(
            CourseDetails.Content content
    ) {
        return new CourseDetailsResponse.ContentResponse(
                content.id(),
                content.title(),
                content.type(),
                content.order(),
                content.durationMinutes(),
                content.isFree(),
                content.url()
        );
    }
}
