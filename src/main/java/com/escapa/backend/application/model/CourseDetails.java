package com.escapa.backend.application.model;

import java.util.List;
import java.util.UUID;

public record CourseDetails(
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
        Instructor instructor,
        List<String> learningObjectives,
        List<Material> materials,
        List<Module> modules
) {

    public record Instructor(
            UUID id,
            String name,
            String headline,
            String bio
    ) {
    }

    public record Material(
            String title,
            String format,
            String fileUrl
    ) {
    }

    public record Module(
            UUID id,
            String title,
            Integer order,
            Integer totalContents,
            Integer durationMinutes,
            List<Content> contents
    ) {
    }

    public record Content(
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