package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.LessonProgressRepositoryPort;
import org.springframework.stereotype.Repository;

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
}
