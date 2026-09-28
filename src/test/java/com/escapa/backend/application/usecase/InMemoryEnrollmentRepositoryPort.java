package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.domain.course.EnrollmentStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

final class InMemoryEnrollmentRepositoryPort implements EnrollmentRepositoryPort {
    private record Key(UUID userId, UUID courseId) {
    }

    private final Map<Key, Enrollment> enrollments = new HashMap<>();
    private final List<StudentCourseCard> courseCards = new ArrayList<>();

    void enroll(UUID userId, UUID courseId, Enrollment enrollment) {
        enrollments.put(new Key(userId, courseId), enrollment);
    }
    
    void addCourseCard(StudentCourseCard card) {
        courseCards.add(card);
    }

    @Override
    public Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId) {
        return Optional.ofNullable(enrollments.get(new Key(userId, courseId)));
    }

    @Override
    public PageResult<StudentCourseCard> findStudentEnrollments(UUID userId, String title, EnrollmentStatus status, int page, int size) {
        List<StudentCourseCard> filtered = courseCards.stream()
                .filter(card -> title == null || title.isBlank() || card.title().toLowerCase().contains(title.toLowerCase()))
                .filter(card -> status == null || card.enrollmentStatus() == status)
                .collect(Collectors.toList());

        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min((page + 1) * size, filtered.size());
        List<StudentCourseCard> content = filtered.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) filtered.size() / size);
        return new PageResult<>(content, page, size, filtered.size(), totalPages);
    }
}
