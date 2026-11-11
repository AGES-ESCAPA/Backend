package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.port.LessonRepositoryPort;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.escapa.backend.application.model.LessonSupplement;
import java.util.List;
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
        return contentJpaRepository.findByIdAndCourseId(lessonId, courseId)
                .filter(LessonRepositoryAdapter::visibleToStudents)
                .map(entity -> {
                    final ModuleEntity module = entity.getModule();
                    final CourseEntity course = module.getCourse();
                    final Content content = ContentMapper.toDomain(entity);
                    final boolean published = course.getStatus() == CourseStatus.PUBLISHED;
                    if (published) {
                        content.setOrder(entity.getPublishedOrder());
                    }
                    final Integer moduleOrder = published ? module.getPublishedOrder() : module.getOrder();
                    return new LessonDetails(
                            content,
                            course.getId(),
                            module.getTitle(),
                            moduleOrder,
                            Boolean.TRUE.equals(course.getEnforceDeadlineBlock()),
                            new LessonSupplement(List.of(), List.of())
                    );
                });
    }

    private static boolean visibleToStudents(ContentEntity entity) {
        final CourseEntity course = entity.getModule().getCourse();
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            return true;
        }
        return entity.getPublishedOrder() != null && entity.getModule().getPublishedOrder() != null;
    }
}
