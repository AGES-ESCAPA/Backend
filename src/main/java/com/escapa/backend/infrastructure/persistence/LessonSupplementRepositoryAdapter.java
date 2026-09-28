package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.LessonConcept;
import com.escapa.backend.application.model.LessonReference;
import com.escapa.backend.application.model.LessonSupplement;
import com.escapa.backend.application.port.LessonSupplementRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class LessonSupplementRepositoryAdapter implements LessonSupplementRepositoryPort {

    private final ContentConceptJpaRepository conceptJpaRepository;
    private final ContentReferenceJpaRepository referenceJpaRepository;

    public LessonSupplementRepositoryAdapter(
            ContentConceptJpaRepository conceptJpaRepository,
            ContentReferenceJpaRepository referenceJpaRepository) {
        this.conceptJpaRepository = conceptJpaRepository;
        this.referenceJpaRepository = referenceJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public LessonSupplement getSupplementsByLessonId(UUID lessonId) {
        final List<LessonConcept> concepts = conceptJpaRepository.findByIdContentIdOrderByIdOrderAsc(lessonId).stream()
                .map(entity -> new LessonConcept(entity.getName(), entity.getId().getOrder()))
                .collect(Collectors.toList());

        final List<LessonReference> references = referenceJpaRepository.findByContentIdOrderByOrderAsc(lessonId).stream()
                .map(entity -> new LessonReference(entity.getId(), entity.getTitle(), entity.getUrl(), entity.getOrder()))
                .collect(Collectors.toList());

        return new LessonSupplement(concepts, references);
    }
}
