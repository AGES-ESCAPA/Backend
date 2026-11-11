package com.escapa.backend.application.usecase;

import com.escapa.backend.application.course.PublishedCourseDrafts;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CourseNotificationPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.course.CourseValidationException;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PublishCourseUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishCourseUseCase.class);


    private final CourseRepositoryPort courseRepository;
    private final CourseChangeLogRepositoryPort changeLogRepository;
    private final CourseNotificationPort courseNotificationPort;
    private final UserRepositoryPort userRepository;
    private final CoursePrerequisiteRepositoryPort prerequisiteRepository;

    public PublishCourseUseCase(
            CourseRepositoryPort courseRepository,
            CourseChangeLogRepositoryPort changeLogRepository,
            CourseNotificationPort courseNotificationPort,
            UserRepositoryPort userRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository) {
        this.courseRepository = courseRepository;
        this.changeLogRepository = changeLogRepository;
        this.courseNotificationPort = courseNotificationPort;
        this.userRepository = userRepository;
        this.prerequisiteRepository = prerequisiteRepository;
    }

    public Course execute(UUID id, boolean notifyEnrolledStudents, User changedBy) {
        final Course course = courseRepository.findById(id).orElseThrow(() -> new CourseNotFoundException(id));
        final CoursePendingDraft draft = PublishedCourseDrafts.isPublished(course) ? course.getPendingDraft() : null;
        final Course candidate = draft == null ? course : PublishedCourseDrafts.editorView(course);

        final List<String> missingFields = new ArrayList<>();
        validateBasicFields(candidate, missingFields);
        validateDetails(candidate, missingFields);
        validateContent(candidate, missingFields);
        if (!missingFields.isEmpty()) {
            throw new CourseValidationException("Cannot publish course due to missing requirements", missingFields);
        }

        applyDraft(course, draft);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setMajorVersion(course.getMajorVersion() + 1);
        course.setMinorVersion(0);
        course.setUpdatedAt(LocalDateTime.now());
        final Course saved = courseRepository.save(course);
        publishPrerequisites(id, draft);

        changeLogRepository.save(id, changedBy != null ? changedBy.getId() : null,
                "Versão " + saved.getMajorVersion() + ".0 publicada.",
                saved.getMajorVersion(), saved.getMinorVersion());

        if (notifyEnrolledStudents) {
            courseNotificationPort.notifyCoursePublished(id);
        }

        LOGGER.info("Course {} published as version {}.0 (notify enrolled students: {})",
                saved.getId(), saved.getMajorVersion(), notifyEnrolledStudents);

        return saved;
    }

    private void applyDraft(Course course, CoursePendingDraft draft) {
        if (draft == null) {
            return;
        }
        final User instructor = resolveInstructor(course, draft.instructorId());
        draft.applyTo(course);
        course.setInstructor(instructor);
        course.setPendingDraft(null);
    }

    private void publishPrerequisites(UUID courseId, CoursePendingDraft draft) {
        if (draft == null) {
            return;
        }
        final List<UUID> prerequisiteIds = draft.prerequisiteIds() == null ? List.of() : draft.prerequisiteIds();
        prerequisiteRepository.replaceAll(courseId, prerequisiteIds);
    }

    private User resolveInstructor(Course course, UUID instructorId) {
        if (instructorId == null) {
            return course.getInstructor();
        }
        if (course.getInstructor() != null && instructorId.equals(course.getInstructor().getId())) {
            return course.getInstructor();
        }
        return userRepository.findById(instructorId)
                .orElseThrow(() -> new UserNotFoundException(instructorId));
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
