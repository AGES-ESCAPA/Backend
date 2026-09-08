package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UpdateCourseUseCase {
    private final CourseRepositoryPort courseRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    public UpdateCourseUseCase(CourseRepositoryPort courseRepositoryPort, UserRepositoryPort userRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    public Course execute(UUID id, String title, String shortDescription, String description, String thumbnailUrl,
                          String teaserVideoUrl, UUID instructorId, String category, String level,
                          Integer durationTime, Integer deadline, Integer accessDurationDays, Double price,
                          List<String> learningObjectives, Boolean requireSequentialProgress,
                          Boolean enforceDeadlineBlock) {

        final Course course = courseRepositoryPort.findById(id).orElseThrow(() -> new CourseNotFoundException(id));

        updateBasicFields(course, title, shortDescription, description, thumbnailUrl, teaserVideoUrl);
        updateInstructor(course, instructorId);
        updateDetails(course, category, level, durationTime, deadline, accessDurationDays, price);
        updateSettings(course, learningObjectives, requireSequentialProgress, enforceDeadlineBlock);

        course.setUpdatedAt(LocalDateTime.now());
        return courseRepositoryPort.save(course);
    }

    private void updateBasicFields(Course course, String title, String shortDescription, String description,
                                   String thumbnailUrl, String teaserVideoUrl) {
        if (title != null && !title.trim().isBlank()) {
            course.setTitle(title.trim());
        }
        if (shortDescription != null) {
            course.setShortDescription(shortDescription);
        }
        if (description != null) {
            course.setDescription(description);
        }
        if (thumbnailUrl != null) {
            course.setThumbnailUrl(thumbnailUrl);
        }
        if (teaserVideoUrl != null) {
            course.setTeaserVideoUrl(teaserVideoUrl);
        }
    }

    private void updateInstructor(Course course, UUID instructorId) {
        if (instructorId != null) {
            final User instructor = userRepositoryPort.findById(instructorId)
                    .orElseThrow(() -> new UserNotFoundException(instructorId));
            course.setInstructor(instructor);
        }
    }

    private void updateDetails(Course course, String category, String level, Integer durationTime,
                               Integer deadline, Integer accessDurationDays, Double price) {
        if (category != null) {
            course.setCategory(category);
        }
        if (level != null) {
            course.setLevel(level);
        }
        if (durationTime != null) {
            course.setDurationTime(durationTime);
        }
        if (deadline != null) {
            course.setDeadline(deadline);
        }
        if (accessDurationDays != null) {
            course.setAccessDurationDays(accessDurationDays);
        }
        if (price != null) {
            course.setPrice(price);
        }
    }

    private void updateSettings(Course course, List<String> learningObjectives, Boolean requireSequentialProgress,
                                Boolean enforceDeadlineBlock) {
        if (learningObjectives != null) {
            course.setLearningObjectives(learningObjectives);
        }
        if (requireSequentialProgress != null) {
            course.setRequireSequentialProgress(requireSequentialProgress);
        }
        if (enforceDeadlineBlock != null) {
            course.setEnforceDeadlineBlock(enforceDeadlineBlock);
        }
    }
}

