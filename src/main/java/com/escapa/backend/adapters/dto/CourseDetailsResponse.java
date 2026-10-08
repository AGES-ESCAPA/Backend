package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.CourseDetails;
import java.util.List;
import java.util.UUID;

public record CourseDetailsResponse(
        UUID id,
        String title,
        String shortDescription,
        String description,
        String category,
        String level,
        Integer durationTime,
        Double price,
        Integer deadline,
        String thumbnailUrl,
        String teaserVideoUrl,
        Double rating,
        Integer reviewsCount,
        Integer studentsCount,
        InstructorResponse instructor,
        List<String> learningObjectives,
        List<MaterialResponse> materials,
        List<ModuleResponse> modules
) {

    public record InstructorResponse(
            UUID id,
            String name,
            String headline,
            String bio,
            String avatarUrl
    ) {
        static InstructorResponse from(CourseDetails.Instructor instructor) {
            if (instructor == null) {
                return null;
            }
            return new InstructorResponse(
                    instructor.id(), instructor.name(), instructor.headline(), instructor.bio(), instructor.avatarUrl());
        }
    }

    public record MaterialResponse(
            String title,
            String format,
            String fileUrl
    ) {
        static MaterialResponse from(CourseDetails.Material material) {
            return new MaterialResponse(material.title(), material.format(), material.fileUrl());
        }
    }

    public record ModuleResponse(
            UUID id,
            String title,
            Integer order,
            Integer totalContents,
            Integer durationMinutes,
            List<ContentResponse> contents
    ) {
        static ModuleResponse from(CourseDetails.Module module) {
            return new ModuleResponse(
                    module.id(),
                    module.title(),
                    module.order(),
                    module.totalContents(),
                    module.durationMinutes(),
                    module.contents().stream().map(ContentResponse::from).toList());
        }
    }

    public record ContentResponse(
            UUID id,
            String title,
            String type,
            Integer order,
            Integer durationMinutes,
            Boolean isFree,
            String url
    ) {
        static ContentResponse from(CourseDetails.Content content) {
            return new ContentResponse(
                    content.id(),
                    content.title(),
                    content.type(),
                    content.order(),
                    content.durationMinutes(),
                    content.isFree(),
                    content.url());
        }
    }


    public static CourseDetailsResponse from(CourseDetails course) {
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
                course.teaserVideoUrl(),
                course.rating(),
                course.reviewsCount(),
                course.studentsCount(),
                InstructorResponse.from(course.instructor()),
                course.learningObjectives(),
                course.materials().stream().map(MaterialResponse::from).toList(),
                course.modules().stream().map(ModuleResponse::from).toList()
        );
    }
}
