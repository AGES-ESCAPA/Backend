package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CourseMapper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private CourseMapper() {
    }

    public static CourseEntity toEntity(Course course) {
        if (course == null) {
            return null;
        }

        final CourseEntity entity = new CourseEntity();
        entity.setId(course.getId() != null ? course.getId() : UUID.randomUUID());
        entity.setTitle(course.getTitle());
        entity.setDescription(course.getDescription());
        entity.setShortDescription(course.getShortDescription());
        entity.setThumbnailUrl(course.getThumbnailUrl());
        entity.setTeaserVideoUrl(course.getTeaserVideoUrl());

        if (course.getStatus() != null) {
            entity.setStatus(CourseStatus.valueOf(course.getStatus().name()));
        }

        entity.setCreatedBy(UserMapper.toEntity(course.getCreatedBy()));
        mapEntityDetails(course, entity);
        return entity;
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
                entity.setLearningObjectives(objectMapper.writeValueAsString(course.getLearningObjectives()));
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
        return course;
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
                course.setLearningObjectives(objectMapper.readValue(entity.getLearningObjectives(), new TypeReference<List<String>>() {}));
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
    }
}
