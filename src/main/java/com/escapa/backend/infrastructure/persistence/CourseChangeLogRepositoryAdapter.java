package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CourseChangeLogRepositoryAdapter implements CourseChangeLogRepositoryPort {

    private final CourseChangeLogJpaRepository repository;
    private final EntityManager entityManager;

    public CourseChangeLogRepositoryAdapter(CourseChangeLogJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public List<ChangeLogEntry> findRecentByCourseId(UUID courseId, int limit) {
        return repository.findByCourseIdOrderByCreatedAtDesc(courseId, PageRequest.of(0, limit))
                .getContent().stream()
                .map(CourseChangeLogRepositoryAdapter::toEntry)
                .toList();
    }

    @Override
    public PageResult<ChangeLogEntry> findPageByCourseId(UUID courseId, int page, int size) {
        final Page<CourseChangeLogEntity> result =
                repository.findByCourseIdOrderByCreatedAtDesc(courseId, PageRequest.of(page, size));
        final List<ChangeLogEntry> content = result.getContent().stream()
                .map(CourseChangeLogRepositoryAdapter::toEntry)
                .toList();
        return new PageResult<>(
                content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public void save(UUID courseId, UUID changedById, String description, int majorVersion, int minorVersion) {
        final CourseEntity course = entityManager.getReference(CourseEntity.class, courseId);
        final UserEntity changedBy = changedById != null
                ? entityManager.getReference(UserEntity.class, changedById)
                : null;
        repository.save(new CourseChangeLogEntity(
                null, course, changedBy, description, majorVersion, minorVersion, LocalDateTime.now()));
    }

    private static ChangeLogEntry toEntry(CourseChangeLogEntity entity) {
        return new ChangeLogEntry(
                entity.getId(),
                entity.getDescription(),
                entity.getChangedBy() == null ? "Sistema" : entity.getChangedBy().getName(),
                entity.getMajorVersion(),
                entity.getMinorVersion(),
                entity.getCreatedAt());
    }
}
