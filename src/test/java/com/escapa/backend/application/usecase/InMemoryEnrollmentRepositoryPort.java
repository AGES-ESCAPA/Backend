package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CourseEnrollmentCount;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.course.EnrollmentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class InMemoryEnrollmentRepositoryPort implements EnrollmentRepositoryPort {
    private record Key(UUID userId, UUID courseId) {
    }

    private record CourseRecord(UUID courseId, String title, String category, CourseStatus status) {
    }

    private record EnrollmentRecord(Enrollment enrollment, LocalDateTime revokedAt) {
    }

    private final Map<Key, EnrollmentRecord> enrollments = new HashMap<>();
    private final Map<UUID, CourseRecord> courses = new HashMap<>();
    private final List<StudentCourseCard> courseCards = new ArrayList<>();

    public void registerCourse(UUID courseId, String title, String category, CourseStatus status) {
        courses.put(courseId, new CourseRecord(courseId, title, category, status));
    }

    public void enroll(UUID userId, UUID courseId, Enrollment enrollment) {
        enroll(userId, courseId, enrollment, null);
    }

    public void enroll(UUID userId, UUID courseId, Enrollment enrollment, LocalDateTime revokedAt) {
        enrollments.put(new Key(userId, courseId), new EnrollmentRecord(enrollment, revokedAt));
    }

    public void addCourseCard(StudentCourseCard card) {
        courseCards.add(card);
    }

    @Override
    public Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId) {
        final EnrollmentRecord record = enrollments.get(new Key(userId, courseId));
        if (record == null || record.revokedAt() != null) {
            return Optional.empty();
        }
        return Optional.of(record.enrollment());
    }

    @Override
    public PageResult<StudentCourseCard> findStudentEnrollments(
            UUID userId, String title, EnrollmentStatus status, int page, int size) {
        final List<StudentCourseCard> filtered = courseCards.stream()
                .filter(card -> title == null || title.isBlank()
                        || card.title().toLowerCase().contains(title.toLowerCase()))
                .filter(card -> status == null || card.enrollmentStatus() == status)
                .collect(Collectors.toList());

        final int fromIndex = Math.min(page * size, filtered.size());
        final int toIndex = Math.min((page + 1) * size, filtered.size());
        final List<StudentCourseCard> content = filtered.subList(fromIndex, toIndex);

        final int totalPages = (int) Math.ceil((double) filtered.size() / size);
        return new PageResult<>(content, page, size, filtered.size(), totalPages);
    }

    @Override
    public List<CourseEnrollmentCount> countEligibleEnrollmentsByCourse() {
        final Map<UUID, Long> countsByCourse = new HashMap<>();
        for (final Map.Entry<Key, EnrollmentRecord> entry : enrollments.entrySet()) {
            if (entry.getValue().revokedAt() != null) {
                continue;
            }
            final CourseRecord course = courses.get(entry.getKey().courseId());
            if (course == null || course.status() == CourseStatus.DRAFT) {
                continue;
            }
            countsByCourse.merge(course.courseId(), 1L, Long::sum);
        }

        final List<CourseEnrollmentCount> result = new ArrayList<>();
        for (final Map.Entry<UUID, Long> countEntry : countsByCourse.entrySet()) {
            final CourseRecord course = courses.get(countEntry.getKey());
            result.add(new CourseEnrollmentCount(
                    course.courseId(), course.title(), course.category(), countEntry.getValue()));
        }
        return result;
    }
}
