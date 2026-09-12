package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CourseDetailsResponse;
import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.usecase.GetCourseDetailsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/courses")
public class PublicCourseController {

    private final GetCourseDetailsUseCase getCourseDetailsUseCase;

    public PublicCourseController(
            GetCourseDetailsUseCase getCourseDetailsUseCase
    ) {
        this.getCourseDetailsUseCase = getCourseDetailsUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDetailsResponse>> getById(
            @PathVariable UUID id
    ) {
        final CourseDetails course = getCourseDetailsUseCase.execute(id);

        return ResponseEntity.ok(
                ApiResponse.success(toResponse(course))
        );
    }

    private static CourseDetailsResponse toResponse(CourseDetails course) {
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