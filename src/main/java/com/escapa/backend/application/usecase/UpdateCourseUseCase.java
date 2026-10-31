package com.escapa.backend.application.usecase;

import com.escapa.backend.application.course.PublishedCourseDrafts;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UpdateCourseUseCase {

    private final CourseRepositoryPort courseRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepositoryPort;

    public UpdateCourseUseCase(
            CourseRepositoryPort courseRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            CoursePrerequisiteRepositoryPort prerequisiteRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.prerequisiteRepositoryPort = prerequisiteRepositoryPort;
    }

    public Course execute(UUID id, String title, String shortDescription, String description, String thumbnailUrl,
                          String teaserVideoUrl, UUID instructorId, String category, String level,
                          Integer durationTime, Integer deadline, Double price,
                          List<String> learningObjectives, Boolean requireSequentialProgress,
                          Boolean enforceDeadlineBlock, User changedBy) {

        final Course course = courseRepositoryPort.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
        if (PublishedCourseDrafts.isPublished(course)) {
            return savePublishedDraft(course, title, shortDescription, description, thumbnailUrl, teaserVideoUrl,
                    instructorId, category, level, durationTime, deadline, price, learningObjectives,
                    requireSequentialProgress, enforceDeadlineBlock);
        }

        updateBasicFields(course, title, shortDescription, description, thumbnailUrl, teaserVideoUrl);
        updateInstructor(course, instructorId);
        updateDetails(course, category, level, durationTime, deadline, price);
        updateSettings(course, learningObjectives, requireSequentialProgress, enforceDeadlineBlock);
        course.setUpdatedAt(LocalDateTime.now());
        return courseRepositoryPort.save(course);
    }

    /**
     * Curso já publicado: a alteração fica no rascunho. Título, preço, descrição e
     * o restante do que os alunos veem só mudam em {@code PublishCourseUseCase}.
     */
    private Course savePublishedDraft(Course course, String title, String shortDescription, String description,
                                      String thumbnailUrl, String teaserVideoUrl, UUID instructorId, String category,
                                      String level, Integer durationTime, Integer deadline, Double price,
                                      List<String> learningObjectives, Boolean requireSequentialProgress,
                                      Boolean enforceDeadlineBlock) {
        if (instructorId != null) {
            requireAdminInstructor(instructorId);
        }
        final CoursePendingDraft draft = PublishedCourseDrafts.ensure(course, prerequisiteRepositoryPort)
                .applyUpdate(title, shortDescription, description, thumbnailUrl, teaserVideoUrl, instructorId,
                        category, level, durationTime, deadline, price, learningObjectives,
                        requireSequentialProgress, enforceDeadlineBlock);
        course.setPendingDraft(draft);
        course.setUpdatedAt(LocalDateTime.now());
        return PublishedCourseDrafts.editorView(courseRepositoryPort.save(course));
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
        // null mantém a capa atual; string vazia remove. A URL nova substitui.
        if (thumbnailUrl != null) {
            course.setThumbnailUrl(blankToNull(thumbnailUrl));
        }
        if (teaserVideoUrl != null) {
            course.setTeaserVideoUrl(teaserVideoUrl);
        }
    }

    private static String blankToNull(String value) {
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void updateInstructor(Course course, UUID instructorId) {
        if (instructorId != null) {
            course.setInstructor(requireAdminInstructor(instructorId));
        }
    }

    private User requireAdminInstructor(UUID instructorId) {
        final User instructor = userRepositoryPort.findById(instructorId)
                .orElseThrow(() -> new UserNotFoundException(instructorId));
        if (!instructor.isAdmin()) {
            throw new IllegalArgumentException("instructorId must reference a user with userType ADMIN");
        }
        return instructor;
    }

    private void updateDetails(Course course, String category, String level, Integer durationTime,
                               Integer deadline, Double price) {
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
