package com.escapa.backend.adapters.dto;

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
            String bio
    ) {
    }

    public record MaterialResponse(
            String title,
            String format,
            String fileUrl
    ) {
    }

    public record ModuleResponse(
            UUID id,
            String title,
            Integer order,
            Integer totalContents,
            Integer durationMinutes,
            List<ContentResponse> contents
    ) {
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
    }
}