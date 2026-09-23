package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.port.LessonRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class LessonRepositoryAdapter implements LessonRepositoryPort {

    private final ContentJpaRepository contentJpaRepository;

    public LessonRepositoryAdapter(ContentJpaRepository contentJpaRepository) {
        this.contentJpaRepository = contentJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LessonDetails> findByCourseIdAndId(UUID courseId, UUID lessonId) {
        return contentJpaRepository.findByIdAndCourseId(lessonId, courseId).map(entity -> {
            final ModuleEntity module = entity.getModule();
            final CourseEntity course = module.getCourse();
            return new LessonDetails(
                    ContentMapper.toDomain(entity),
                    course.getId(),
                    module.getTitle(),
                    module.getOrder(),
                    Boolean.TRUE.equals(course.getEnforceDeadlineBlock())
            );
        });
    }
}
