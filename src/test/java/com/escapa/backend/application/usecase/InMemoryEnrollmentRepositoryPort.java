package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.StudentCourseCard;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.domain.course.EnrollmentStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class InMemoryEnrollmentRepositoryPort implements EnrollmentRepositoryPort {
    private record Key(UUID userId, UUID courseId) {
    }

    private final Map<Key, Enrollment> enrollments = new HashMap<>();
    private final List<StudentCourseCard> courseCards = new ArrayList<>();
    private final Map<Key, LocalDateTime> lastAccess = new HashMap<>();
    private final Set<Key> revoked = new HashSet<>();

    public void enroll(UUID userId, UUID courseId, Enrollment enrollment) {
        enrollments.put(new Key(userId, courseId), enrollment);
    }
    
    @Override
    public void recordLastAccess(UUID userId, UUID courseId, LocalDateTime accessedAt, Duration minInterval) {
        final Key key = new Key(userId, courseId);
        final LocalDateTime previous = lastAccess.get(key);
        final boolean due = previous == null || !previous.isAfter(accessedAt.minus(minInterval));
        if (enrollments.containsKey(key) && !revoked.contains(key) && due) {
            lastAccess.put(key, accessedAt);
        }
    }

    public Optional<LocalDateTime> lastAccessOf(UUID userId, UUID courseId) {
        return Optional.ofNullable(lastAccess.get(new Key(userId, courseId)));
    }

    /** Marca a matricula como revogada: some da busca e deixa de receber o ultimo acesso. */
    public void revoke(UUID userId, UUID courseId) {
        revoked.add(new Key(userId, courseId));
    }

    public void addCourseCard(StudentCourseCard card) {
        courseCards.add(card);
    }

    @Override
    public Optional<Enrollment> findByUserIdAndCourseId(UUID userId, UUID courseId) {
        final Key key = new Key(userId, courseId);
        return revoked.contains(key) ? Optional.empty() : Optional.ofNullable(enrollments.get(key));
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
