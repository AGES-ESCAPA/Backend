package com.escapa.backend.adapters.dto;
import java.util.List;
import java.util.UUID;

public record CourseRulesResponse(    
    
    Boolean requireSequentialProgress,
    Boolean enforceDeadlineBlock,
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
}
