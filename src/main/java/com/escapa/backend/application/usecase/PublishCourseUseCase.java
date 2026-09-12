package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.course.CourseValidationException;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PublishCourseUseCase {
    private final CourseRepositoryPort courseRepositoryPort;

    public PublishCourseUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public Course execute(UUID id) {
        final Course course = courseRepositoryPort.findById(id).orElseThrow(() -> new CourseNotFoundException(id));

        final List<String> missingFields = new ArrayList<>();

        validateBasicFields(course, missingFields);
        validateDetails(course, missingFields);
        validateContent(course, missingFields);

        if (!missingFields.isEmpty()) {
            throw new CourseValidationException("Cannot publish course due to missing requirements", missingFields);
        }

        course.setStatus(CourseStatus.PUBLISHED);
        course.setUpdatedAt(LocalDateTime.now());

        return courseRepositoryPort.save(course);
    }

    private void validateBasicFields(Course course, List<String> missingFields) {
        if (course.getTitle() == null || course.getTitle().isBlank()) {
            missingFields.add("title");
        }
        if (course.getDescription() == null || course.getDescription().isBlank()) {
            missingFields.add("description");
        }
        if (course.getShortDescription() == null || course.getShortDescription().isBlank()) {
            missingFields.add("shortDescription");
        }
        if (course.getInstructor() == null) {
            missingFields.add("instructorId");
        }
    }

    private void validateDetails(Course course, List<String> missingFields) {
        if (course.getCategory() == null || course.getCategory().isBlank()) {
            missingFields.add("category");
        }
        if (course.getLevel() == null || course.getLevel().isBlank()) {
            missingFields.add("level");
        }
        if (course.getPrice() == null) {
            missingFields.add("price");
        }
        if (course.getDurationTime() == null) {
            missingFields.add("durationTime");
        }
        if (course.getDeadline() == null) {
            missingFields.add("deadline");
        }
        if (course.getAccessDurationDays() == null) {
            missingFields.add("accessDurationDays");
        }
    }

    private void validateContent(Course course, List<String> missingFields) {
        boolean hasContent = false;
        if (course.getModules() != null && !course.getModules().isEmpty()) {
            for (final Module module : course.getModules()) {
                if (module.getContents() != null && !module.getContents().isEmpty()) {
                    hasContent = true;
                    break;
                }
            }
        }

        if (!hasContent) {
            missingFields.add("modules (at least one module with content is required)");
        }
    }
}

