package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.LessonProgressRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.UserContentProgressEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserContentProgressId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class LessonProgressRepositoryAdapter implements LessonProgressRepositoryPort {

    private final UserContentProgressJpaRepository progressJpaRepository;

    public LessonProgressRepositoryAdapter(UserContentProgressJpaRepository progressJpaRepository) {
        this.progressJpaRepository = progressJpaRepository;
    }

    @Override
    public List<UUID> findCompletedLessonIds(UUID userId, UUID courseId) {
        return progressJpaRepository.findCompletedContentIds(userId, courseId);
    }

    @Override
    @Transactional
    public void markCompleted(UUID userId, UUID lessonId, LocalDateTime completedAt) {
        final UserContentProgressId id = new UserContentProgressId(userId, lessonId);
        if (progressJpaRepository.existsById(id)) {
            return;
        }
        progressJpaRepository.save(new UserContentProgressEntity(id, completedAt));
    }
}
