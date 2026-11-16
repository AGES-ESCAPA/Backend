package com.escapa.backend.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CompanyCourseSummary(
    UUID id,
    String title,
    String shortDescription,
    String thumbnailUrl,
    String category,
    String level,
    Integer durationTime,
    Integer lessonsCount,
    String instructorName,
    Double rating,
    LocalDate acquiredAt,
    Long employeesWithAccess
) {

}
