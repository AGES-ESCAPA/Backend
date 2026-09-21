package com.escapa.backend.application.usecase;

import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class UpdateCourseUseCase {
    private final CourseRepositoryPort courseRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final CourseChangeLogRepositoryPort changeLogRepositoryPort;

    public UpdateCourseUseCase(
            CourseRepositoryPort courseRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            CourseChangeLogRepositoryPort changeLogRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.changeLogRepositoryPort = changeLogRepositoryPort;
    }

    public Course execute(UUID id, String title, String shortDescription, String description, String thumbnailUrl,
                          String teaserVideoUrl, UUID instructorId, String category, String level,
                          Integer durationTime, Integer deadline, Integer accessDurationDays, Double price,
                          List<String> learningObjectives, Boolean requireSequentialProgress,
                          Boolean enforceDeadlineBlock, User changedBy) {

        final Course course = courseRepositoryPort.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
        final boolean wasPublished = course.getStatus() == CourseStatus.PUBLISHED;
        final Snapshot before = Snapshot.of(course);

        updateBasicFields(course, title, shortDescription, description, thumbnailUrl, teaserVideoUrl);
        updateInstructor(course, instructorId);
        updateDetails(course, category, level, durationTime, deadline, accessDurationDays, price);
        updateSettings(course, learningObjectives, requireSequentialProgress, enforceDeadlineBlock);

        final boolean changed = wasPublished && before.differsFrom(course);
        if (changed) {
            course.setMinorVersion(course.getMinorVersion() + 1);
        }
        course.setUpdatedAt(LocalDateTime.now());

        final Course saved = courseRepositoryPort.save(course);

        if (changed) {
            changeLogRepositoryPort.save(id, changedBy != null ? changedBy.getId() : null,
                    "Curso atualizado.", saved.getMajorVersion(), saved.getMinorVersion());
        }

        return saved;
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
            if (!"ADMIN".equalsIgnoreCase(instructor.getUserType())) {
                throw new IllegalArgumentException("instructorId must reference a user with userType ADMIN");
            }
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

    /**
     * Estado do curso relevante para versionamento (US-09), capturado antes das
     * atualizações parciais para decidir se algo realmente mudou.
     */
    private record Snapshot(
            String title, String shortDescription, String description, String thumbnailUrl, String teaserVideoUrl,
            UUID instructorId, String category, String level, Integer durationTime, Integer deadline,
            Integer accessDurationDays, Double price, List<String> learningObjectives,
            Boolean requireSequentialProgress, Boolean enforceDeadlineBlock) {

        static Snapshot of(Course course) {
            return new Snapshot(
                    course.getTitle(), course.getShortDescription(), course.getDescription(),
                    course.getThumbnailUrl(), course.getTeaserVideoUrl(),
                    course.getInstructor() != null ? course.getInstructor().getId() : null,
                    course.getCategory(), course.getLevel(), course.getDurationTime(), course.getDeadline(),
                    course.getAccessDurationDays(), course.getPrice(), List.copyOf(course.getLearningObjectives()),
                    course.getRequireSequentialProgress(), course.getEnforceDeadlineBlock());
        }

        boolean differsFrom(Course course) {
            final UUID afterInstructorId = course.getInstructor() != null ? course.getInstructor().getId() : null;
            return !Objects.equals(title, course.getTitle())
                    || !Objects.equals(shortDescription, course.getShortDescription())
                    || !Objects.equals(description, course.getDescription())
                    || !Objects.equals(thumbnailUrl, course.getThumbnailUrl())
                    || !Objects.equals(teaserVideoUrl, course.getTeaserVideoUrl())
                    || !Objects.equals(instructorId, afterInstructorId)
                    || !Objects.equals(category, course.getCategory())
                    || !Objects.equals(level, course.getLevel())
                    || !Objects.equals(durationTime, course.getDurationTime())
                    || !Objects.equals(deadline, course.getDeadline())
                    || !Objects.equals(accessDurationDays, course.getAccessDurationDays())
                    || !Objects.equals(price, course.getPrice())
                    || !Objects.equals(learningObjectives, course.getLearningObjectives())
                    || !Objects.equals(requireSequentialProgress, course.getRequireSequentialProgress())
                    || !Objects.equals(enforceDeadlineBlock, course.getEnforceDeadlineBlock());
        }
    }
}
