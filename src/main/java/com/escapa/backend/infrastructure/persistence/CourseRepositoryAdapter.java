package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.AdminEntity;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseMaterialEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class CourseRepositoryAdapter implements CourseRepositoryPort {

    private final CourseJpaRepository courseJpaRepository;
    private final ContentJpaRepository contentJpaRepository;
    private final ObjectMapper objectMapper;

    public CourseRepositoryAdapter(
            CourseJpaRepository courseJpaRepository,
            ContentJpaRepository contentJpaRepository,
            ObjectMapper objectMapper
    ) {
        this.courseJpaRepository = courseJpaRepository;
        this.contentJpaRepository = contentJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<CourseDetails> findDetailsById(UUID id) {
        return courseJpaRepository.findWithModulesById(id)
                .filter(course -> course.getStatus() == CourseStatus.PUBLISHED)
                .map(this::withMaterialsAndContents)
                .map(this::toDetails);
    }

    /**
     * Completa o agregado carregado por {@code findWithModulesById} com
     * materiais e conteudos dos modulos, buscados em consultas separadas
     * para nao combinar mais de uma colecao "bag" (List sem indice) em uma
     * mesma consulta com fetch join (ver MultipleBagFetchException).
     */
    private CourseEntity withMaterialsAndContents(CourseEntity course) {
        courseJpaRepository.findWithMaterialsById(course.getId())
                .ifPresent(withMaterials -> course.setMaterials(withMaterials.getMaterials()));

        final List<ModuleEntity> modules = course.getModules();
        if (!modules.isEmpty()) {
            final List<UUID> moduleIds = modules.stream()
                    .map(ModuleEntity::getId)
                    .toList();
            final Map<UUID, List<ContentEntity>> contentsByModule = contentJpaRepository
                    .findByModule_IdInOrderByOrderAsc(moduleIds)
                    .stream()
                    .collect(Collectors.groupingBy(content -> content.getModule().getId()));

            for (ModuleEntity module : modules) {
                module.setContents(
                        contentsByModule.getOrDefault(module.getId(), List.of())
                );
            }
        }

        return course;
    }

    private CourseDetails toDetails(CourseEntity course) {
        return new CourseDetails(
                course.getId(),
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
                course.getCategory(),
                course.getLevel(),
                course.getDurationTime(),
                course.getPrice(),
                course.getDeadline(),
                course.getThumbnailUrl(),
                course.getRatingAverage(),
                course.getReviewsCount(),
                course.getStudentsCount(),
                toInstructor(course.getInstructor()),
                toLearningObjectives(course.getLearningObjectives()),
                toMaterials(course.getMaterials()),
                toModules(course.getModules())
        );
    }

    private static CourseDetails.Instructor toInstructor(AdminEntity instructor) {
        if (instructor == null) {
            return null;
        }

        return new CourseDetails.Instructor(
                instructor.getId(),
                instructor.getName(),
                instructor.getHeadline(),
                instructor.getBio()
        );
    }

    private static List<CourseDetails.Material> toMaterials(
            List<CourseMaterialEntity> materials
    ) {
        if (materials == null) {
            return List.of();
        }

        return materials.stream()
                .map(material -> new CourseDetails.Material(
                        material.getTitle(),
                        material.getFileType(),
                        material.getFileUrl()
                ))
                .toList();
    }

    private static List<CourseDetails.Module> toModules(
            List<ModuleEntity> modules
    ) {
        if (modules == null) {
            return List.of();
        }

        return modules.stream()
                .map(CourseRepositoryAdapter::toModule)
                .toList();
    }

    private static CourseDetails.Module toModule(ModuleEntity module) {
        final List<CourseDetails.Content> contents =
                toContents(module.getContents());

        final int durationMinutes = contents.stream()
                .map(CourseDetails.Content::durationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        return new CourseDetails.Module(
                module.getId(),
                module.getTitle(),
                module.getOrder(),
                contents.size(),
                durationMinutes,
                contents
        );
    }

    private static List<CourseDetails.Content> toContents(
            List<ContentEntity> contents
    ) {
        if (contents == null) {
            return List.of();
        }

        return contents.stream()
                .map(content -> new CourseDetails.Content(
                        content.getId(),
                        content.getTitle(),
                        content.getType() != null
                                ? content.getType().name()
                                : null,
                        content.getOrder(),
                        content.getDurationMinutes(),
                        content.getIsFree(),
                        Boolean.TRUE.equals(content.getIsFree())
                                ? content.getUrl()
                                : null
                ))
                .toList();
    }

    private List<String> toLearningObjectives(String learningObjectives) {
        if (learningObjectives == null || learningObjectives.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    learningObjectives,
                    new TypeReference<List<String>>() {
                    }
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Invalid learning objectives JSON for course",
                    ex
            );
        }
    }
}
