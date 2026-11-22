package com.escapa.backend.adapters.dto;

import java.util.List;
import java.util.UUID;

import com.escapa.backend.application.model.CourseRules;

public record CourseRulesResponse(    
    
    Boolean requireSequentialProgress,
    Boolean enforceDeadlineBlock,
    Integer minQuizPassPercentage,
    String version,
    List<PrerequisiteResponse> prerequisites,
    List<ChangeLogResponse> changeLog
) {

    public record PrerequisiteResponse(
        String courseId,
        String courseTitle
    ) {
    }

    public record ChangeLogResponse(
        UUID id,
        String description,
        String changedBy,
        String createdAt
    ){
    }


    public static CourseRulesResponse from(CourseRules rules) {
        return new CourseRulesResponse(
                rules.requireSequentialProgress(),
                rules.enforceDeadlineBlock(),
                rules.minQuizPassPercentage(),
                rules.version(),
                rules.prerequisites().stream()
                        .map(p -> new PrerequisiteResponse(p.courseId().toString(), p.courseTitle()))
                        .toList(),
                rules.recentChangeLog().stream()
                        .map(log -> new ChangeLogResponse(
                                log.id(), log.description(), log.changedByName(),
                                log.createdAt() == null ? null : log.createdAt().toString()))
                        .toList());
    }
}
