package com.escapa.backend.infrastructure.persistence.course;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.infrastructure.persistence.ContentMapper;
import com.escapa.backend.infrastructure.persistence.UserMapper;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversor estático entre {@link CourseEntity} (JPA) e as representações de
 * aplicação/domínio de curso: {@link CourseSummary} (listagem pública) e
 * {@link Course} (agregado de domínio usado pelo CRUD administrativo).
 * Segue o mesmo padrão do {@code UserMapper} existente.
 */
public final class CourseMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private CourseMapper() {
    }

    public static CourseSummary toSummary(CourseEntity entity) {
        if (entity == null) {
            return null;
        }
        final String instructorName = entity.getInstructor() != null
                ? entity.getInstructor().getName() : null;

        return new CourseSummary(
                entity.getId(),
                entity.getTitle(),
                entity.getShortDescription(),
                entity.getCategory(),
                entity.getLevel(),
                entity.getDurationTime(),
                entity.getLessonsCount(),
                entity.getPrice(),
                entity.getThumbnailUrl(),
                instructorName,
                entity.getRatingAverage(),
                entity.getReviewsCount()
        );
    }

    /**
     * Aplica sobre {@code entity} apenas os campos que o CRUD administrativo (US-05)
     * edita. Não toca em id, contadores desnormalizados (lessons_count, materials_count,
     * students_count, reviews_count, rating_average, major/minor_version) nem nas
     * associações (modules, materials, reviews, ...): esses campos não fazem parte do
     * agregado de domínio {@link Course} e são mantidos por outros fluxos (conteúdo,
     * matrícula, avaliação). {@code entity} deve ser uma linha existente carregada do
     * banco (update) ou uma {@link CourseEntity} recém-criada com id nulo (create) —
     * nunca reconstruída do zero para um curso que já existe, para não perder esse estado.
     */
    public static void applyToEntity(Course course, CourseEntity entity) {
        if (course == null || entity == null) {
            return;
        }

        entity.setTitle(course.getTitle());
        entity.setDescription(course.getDescription());
        entity.setShortDescription(course.getShortDescription());
        entity.setThumbnailUrl(course.getThumbnailUrl());
        entity.setTeaserVideoUrl(course.getTeaserVideoUrl());

        if (course.getStatus() != null) {
            entity.setStatus(CourseStatus.valueOf(course.getStatus().name()));
        }

        entity.setMajorVersion(course.getMajorVersion());
        entity.setMinorVersion(course.getMinorVersion());

        mapEntityDetails(course, entity);
    }

    private static void mapEntityDetails(Course course, CourseEntity entity) {
        entity.setCategory(course.getCategory());
        entity.setLevel(course.getLevel());
        entity.setDurationTime(course.getDurationTime());
        entity.setDeadline(course.getDeadline());
        entity.setAccessDurationDays(course.getAccessDurationDays());
        entity.setPrice(course.getPrice());

        if (course.getLearningObjectives() != null) {
            try {
                entity.setLearningObjectives(OBJECT_MAPPER.writeValueAsString(course.getLearningObjectives()));
            } catch (JsonProcessingException e) {
                entity.setLearningObjectives("[]");
            }
        }

        entity.setRequireSequentialProgress(course.getRequireSequentialProgress());
        entity.setEnforceDeadlineBlock(course.getEnforceDeadlineBlock());
        entity.setCreatedAt(course.getCreatedAt());
        entity.setUpdatedAt(course.getUpdatedAt());
    }

    public static Course toDomain(CourseEntity entity) {
        if (entity == null) {
            return null;
        }

        final Course course = new Course();
        course.setId(entity.getId());
        course.setTitle(entity.getTitle());
        course.setDescription(entity.getDescription());
        course.setShortDescription(entity.getShortDescription());
        course.setThumbnailUrl(entity.getThumbnailUrl());
        course.setTeaserVideoUrl(entity.getTeaserVideoUrl());

        if (entity.getStatus() != null) {
            course.setStatus(com.escapa.backend.domain.course.CourseStatus.valueOf(entity.getStatus().name()));
        }

        course.setCreatedBy(UserMapper.toDomain(entity.getCreatedBy()));
        if (entity.getInstructor() != null) {
            course.setInstructor(UserMapper.toDomain(entity.getInstructor()));
        }

        mapDomainDetails(entity, course);

        if (entity.getModules() != null) {
            course.setModules(entity.getModules().stream()
                    .map(module -> toModule(module, course))
                    .toList());
        }

        return course;
    }

    private static Module toModule(ModuleEntity entity, Course course) {
        final Module module = new Module(entity.getId(), course, entity.getTitle(), entity.getOrder());
        if (entity.getContents() != null) {
            module.setContents(entity.getContents().stream()
                    .map(ContentMapper::toDomain)
                    .toList());
        }
        return module;
    }

    private static void mapDomainDetails(CourseEntity entity, Course course) {
        course.setCategory(entity.getCategory());
        course.setLevel(entity.getLevel());
        course.setDurationTime(entity.getDurationTime());
        course.setDeadline(entity.getDeadline());
        course.setAccessDurationDays(entity.getAccessDurationDays());
        course.setPrice(entity.getPrice());

        if (entity.getLearningObjectives() != null && !entity.getLearningObjectives().isBlank()) {
            try {
                course.setLearningObjectives(
                        OBJECT_MAPPER.readValue(entity.getLearningObjectives(), new TypeReference<List<String>>() { }));
            } catch (JsonProcessingException e) {
                course.setLearningObjectives(new ArrayList<>());
            }
        } else {
            course.setLearningObjectives(new ArrayList<>());
        }

        course.setRequireSequentialProgress(entity.getRequireSequentialProgress());
        course.setEnforceDeadlineBlock(entity.getEnforceDeadlineBlock());
        course.setCreatedAt(entity.getCreatedAt());
        course.setUpdatedAt(entity.getUpdatedAt());
        course.setMajorVersion(entity.getMajorVersion());
        course.setMinorVersion(entity.getMinorVersion());
    }
}
