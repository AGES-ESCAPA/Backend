package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.UUID;

public class CreateCourseUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCourseUseCase.class);

    private final CourseRepositoryPort courseRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    public CreateCourseUseCase(CourseRepositoryPort courseRepositoryPort, UserRepositoryPort userRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    public Course execute(String title, String shortDescription, String description, String thumbnailUrl,
                          String teaserVideoUrl, UUID instructorId, String category, String level,
                          Integer durationTime, Integer deadline, Integer accessDurationDays, Double price,
                          List<String> learningObjectives, Boolean requireSequentialProgress,
                          Boolean enforceDeadlineBlock, UUID createdById) {

        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        final User instructor = resolveInstructor(instructorId);

        final User createdBy = userRepositoryPort.findById(createdById)
                .orElseThrow(() -> new UserNotFoundException(createdById));

        final Course course = new Course(
                title.trim(),
                shortDescription,
                description,
                thumbnailUrl,
                teaserVideoUrl,
                instructor,
                category,
                level,
                durationTime,
                deadline,
                accessDurationDays,
                price,
                learningObjectives,
                requireSequentialProgress,
                enforceDeadlineBlock,
                createdBy
        );

        final Course saved = courseRepositoryPort.save(course);
        LOGGER.info("Course {} created by user {}", saved.getId(), createdById);
        return saved;
    }

    private User resolveInstructor(UUID instructorId) {
        if (instructorId == null) {
            return null;
        }
        final User instructor = userRepositoryPort.findById(instructorId)
                .orElseThrow(() -> new UserNotFoundException(instructorId));
        if (!instructor.isAdmin()) {
            throw new IllegalArgumentException("instructorId must reference a user with userType ADMIN");
        }
        return instructor;
    }
}
