package com.escapa.backend.adapters.dto;

import java.util.UUID;

import com.escapa.backend.application.model.EmployeeCourseAssignment;

public record EmployeeCourseAssignmentResponse(
        UUID courseId,
        String title,
        String category,
        boolean assigned
) {
    public static EmployeeCourseAssignmentResponse from(EmployeeCourseAssignment assignment) {
        return new EmployeeCourseAssignmentResponse(
                assignment.courseId(),
                assignment.title(),
                assignment.category(),
                assignment.assigned());
    }
}
