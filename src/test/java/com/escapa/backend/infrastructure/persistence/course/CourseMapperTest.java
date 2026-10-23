package com.escapa.backend.infrastructure.persistence.course;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserEntity;
import com.escapa.backend.infrastructure.persistence.entity.AdminEntity;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseMapperTest {

    @Test
    void shouldMapCourseEntityToSummary() {
        final CourseEntity entity = new CourseEntity();
        entity.setId(UUID.randomUUID());
        entity.setTitle("Hospitalidade");
        entity.setShortDescription("Atendimento");
        entity.setCategory("Turismo");
        entity.setLevel("INICIANTE");
        entity.setDurationTime(60);
        entity.setLessonsCount(8);
        entity.setPrice(129.90);
        entity.setThumbnailUrl("/thumb.png");
        entity.setInstructor(new AdminEntity());
        entity.getInstructor().setName("Ana");
        entity.setRatingAverage(4.8);
        entity.setReviewsCount(12);

        final CourseSummary summary = CourseMapper.toSummary(entity);

        assertEquals(entity.getId(), summary.id());
        assertEquals("Hospitalidade", summary.title());
        assertEquals("Atendimento", summary.shortDescription());
        assertEquals("Turismo", summary.category());
        assertEquals("INICIANTE", summary.level());
        assertEquals(60, summary.durationTime());
        assertEquals(8, summary.lessonsCount());
        assertEquals(129.90, summary.price());
        assertEquals("/thumb.png", summary.thumbnailUrl());
        assertEquals("Ana", summary.instructorName());
        assertEquals(4.8, summary.ratingAverage());
        assertEquals(12, summary.reviewsCount());
    }

    @Test
    void shouldReturnNullSummaryForNullEntityAndAllowMissingInstructor() {
        final CourseEntity entity = new CourseEntity();
        entity.setTitle("Curso sem instrutor");

        assertNull(CourseMapper.toSummary(null));
        assertNull(CourseMapper.toSummary(entity).instructorName());
    }

    @Test
    void shouldLeaveEntityUnchangedWhenApplyArgumentsAreNull() {
        final CourseEntity entity = new CourseEntity();
        entity.setTitle("Existing title");

        CourseMapper.applyToEntity(null, entity);
        CourseMapper.applyToEntity(new Course(), null);

        assertEquals("Existing title", entity.getTitle());
    }

    @Test
    void shouldApplyEditableCourseFieldsAndPreserveOtherEntityState() {
        final Course course = createCourse();
        course.setStatus(CourseStatus.PUBLISHED);
        course.setMajorVersion(2);
        course.setMinorVersion(3);
        final CourseEntity entity = new CourseEntity();
        entity.setId(UUID.randomUUID());
        entity.setLessonsCount(7);
        entity.setMaterialsCount(4);
        entity.setStudentsCount(9);
        entity.setReviewsCount(6);
        final List<ModuleEntity> modules = new ArrayList<>();
        entity.setModules(modules);

        CourseMapper.applyToEntity(course, entity);

        assertEquals(course.getTitle(), entity.getTitle());
        assertEquals(course.getDescription(), entity.getDescription());
        assertEquals(course.getShortDescription(), entity.getShortDescription());
        assertEquals(course.getThumbnailUrl(), entity.getThumbnailUrl());
        assertEquals(course.getTeaserVideoUrl(), entity.getTeaserVideoUrl());
        assertEquals(CourseStatus.PUBLISHED, entity.getStatus());
        assertEquals(course.getCategory(), entity.getCategory());
        assertEquals(course.getLevel(), entity.getLevel());
        assertEquals(course.getDurationTime(), entity.getDurationTime());
        assertEquals(course.getDeadline(), entity.getDeadline());
        assertEquals(course.getPrice(), entity.getPrice());
        assertEquals("[\"Objetivo A\",\"Objetivo B\"]", entity.getLearningObjectives());
        assertEquals(course.getRequireSequentialProgress(), entity.getRequireSequentialProgress());
        assertEquals(course.getEnforceDeadlineBlock(), entity.getEnforceDeadlineBlock());
        assertEquals(course.getCreatedAt(), entity.getCreatedAt());
        assertEquals(course.getUpdatedAt(), entity.getUpdatedAt());
        assertEquals(2, entity.getMajorVersion());
        assertEquals(3, entity.getMinorVersion());
        assertEquals(7, entity.getLessonsCount());
        assertEquals(4, entity.getMaterialsCount());
        assertEquals(9, entity.getStudentsCount());
        assertEquals(6, entity.getReviewsCount());
        assertSame(modules, entity.getModules());
    }

    @Test
    void shouldKeepEntityStatusAndLearningObjectivesWhenDomainValuesAreNull() {
        final Course course = createCourse();
        course.setStatus(null);
        course.setLearningObjectives(null);
        final CourseEntity entity = new CourseEntity();
        entity.setStatus(CourseStatus.ARCHIVED);
        entity.setLearningObjectives("[\"Existente\"]");

        CourseMapper.applyToEntity(course, entity);

        assertEquals(CourseStatus.ARCHIVED, entity.getStatus());
        assertEquals("[\"Existente\"]", entity.getLearningObjectives());
    }

    @Test
    void shouldIgnoreNullArgumentsWhenApplyingCourseToEntity() {
        final CourseEntity entity = new CourseEntity();
        entity.setTitle("Mantido");

        CourseMapper.applyToEntity(null, entity);
        CourseMapper.applyToEntity(createCourse(), null);

        assertEquals("Mantido", entity.getTitle());
    }

    @Test
    void shouldMapEntityToDomainIncludingUsersModulesAndContents() {
        final CourseEntity entity = createCourseEntity();
        final UserEntity creator = createUserEntity("Criador", "creator@example.com");
        final AdminEntity instructor = createInstructorEntity("Instrutora");
        entity.setCreatedBy(creator);
        entity.setInstructor(instructor);
        entity.setStatus(CourseStatus.PUBLISHED);
        entity.setLearningObjectives("[\"Objetivo A\",\"Objetivo B\"]");
        final ModuleEntity moduleEntity = new ModuleEntity(
                UUID.randomUUID(), entity, "Módulo", 1, new ArrayList<>(), new ArrayList<>());
        final ContentEntity contentEntity = new ContentEntity(UUID.randomUUID(), moduleEntity, "Aula",
                "Descrição", ContentType.VIDEO, "https://example.com/aula", 15, true, 1,
                null, LocalDateTime.of(2025, 1, 2, 3, 4));
        moduleEntity.setContents(List.of(contentEntity));
        entity.setModules(List.of(moduleEntity));

        final Course course = CourseMapper.toDomain(entity);

        assertEquals(entity.getId(), course.getId());
        assertEquals(entity.getTitle(), course.getTitle());
        assertEquals(entity.getDescription(), course.getDescription());
        assertEquals(entity.getShortDescription(), course.getShortDescription());
        assertEquals(entity.getThumbnailUrl(), course.getThumbnailUrl());
        assertEquals(entity.getTeaserVideoUrl(), course.getTeaserVideoUrl());
        assertEquals(CourseStatus.PUBLISHED, course.getStatus());
        assertEquals("Criador", course.getCreatedBy().getName());
        assertEquals("Instrutora", course.getInstructor().getName());
        assertEquals(entity.getCategory(), course.getCategory());
        assertEquals(entity.getLevel(), course.getLevel());
        assertEquals(entity.getDurationTime(), course.getDurationTime());
        assertEquals(entity.getDeadline(), course.getDeadline());
        assertEquals(entity.getPrice(), course.getPrice());
        assertEquals(List.of("Objetivo A", "Objetivo B"), course.getLearningObjectives());
        assertEquals(entity.getRequireSequentialProgress(), course.getRequireSequentialProgress());
        assertEquals(entity.getEnforceDeadlineBlock(), course.getEnforceDeadlineBlock());
        assertEquals(entity.getCreatedAt(), course.getCreatedAt());
        assertEquals(entity.getUpdatedAt(), course.getUpdatedAt());
        assertEquals(entity.getMajorVersion(), course.getMajorVersion());
        assertEquals(entity.getMinorVersion(), course.getMinorVersion());
        final Module module = course.getModules().get(0);
        assertSame(course, module.getCourse());
        assertEquals("Módulo", module.getTitle());
        final Content content = module.getContents().get(0);
        assertEquals(contentEntity.getId(), content.getId());
        assertEquals(moduleEntity.getId(), content.getModuleId());
        assertEquals("Aula", content.getTitle());
        assertEquals(ContentType.VIDEO, content.getType());
        assertEquals("https://example.com/aula", content.getUrl());
        assertEquals(15, content.getDurationMinutes());
        assertEquals(true, content.getIsFree());
        assertEquals(1, content.getOrder());
        assertEquals(contentEntity.getCreatedAt(), content.getCreatedAt());
    }

    @Test
    void shouldUseEmptyCollectionsForMissingOrInvalidEntityValues() {
        final CourseEntity entity = createCourseEntity();
        entity.setStatus(null);
        entity.setCreatedBy(null);
        entity.setInstructor(null);
        entity.setLearningObjectives("not-json");
        entity.setModules(null);

        final Course course = CourseMapper.toDomain(entity);

        assertEquals(CourseStatus.DRAFT, course.getStatus());
        assertNull(course.getCreatedBy());
        assertNull(course.getInstructor());
        assertTrue(course.getLearningObjectives().isEmpty());
        assertTrue(course.getModules().isEmpty());
        assertNull(CourseMapper.toDomain(null));

        entity.setLearningObjectives("  ");
        assertTrue(CourseMapper.toDomain(entity).getLearningObjectives().isEmpty());
    }

    @Test
    void shouldMapListingRowToDomainCourse() {
        final UUID id = UUID.randomUUID();
        final CourseJpaRepository.ListingRow row = listingRow(
                id, "Curso", "Turismo", 49.90, CourseStatus.PUBLISHED, 3, 2);

        final Course course = CourseMapper.toListingDomain(row);

        assertEquals(id, course.getId());
        assertEquals("Curso", course.getTitle());
        assertEquals("Turismo", course.getCategory());
        assertEquals(49.90, course.getPrice());
        assertEquals(CourseStatus.PUBLISHED, course.getStatus());
        assertEquals(3, course.getMajorVersion());
        assertEquals(2, course.getMinorVersion());
    }

    private static Course createCourse() {
        final Course course = new Course("Curso", "Resumo", "Descrição", "/thumb.png",
                "/teaser.mp4", null, "Turismo", "INICIANTE", 90, 30, 99.90,
                List.of("Objetivo A", "Objetivo B"), false, true, null);
        course.setCreatedAt(LocalDateTime.of(2025, 1, 2, 3, 4));
        course.setUpdatedAt(LocalDateTime.of(2025, 2, 3, 4, 5));
        return course;
    }

    private static CourseEntity createCourseEntity() {
        final CourseEntity entity = new CourseEntity();
        entity.setId(UUID.randomUUID());
        entity.setTitle("Curso");
        entity.setDescription("Descrição");
        entity.setShortDescription("Resumo");
        entity.setThumbnailUrl("/thumb.png");
        entity.setTeaserVideoUrl("/teaser.mp4");
        entity.setCategory("Turismo");
        entity.setLevel("AVANÇADO");
        entity.setDurationTime(90);
        entity.setDeadline(45);
        entity.setPrice(99.90);
        entity.setRequireSequentialProgress(false);
        entity.setEnforceDeadlineBlock(true);
        entity.setCreatedAt(LocalDateTime.of(2025, 1, 2, 3, 4));
        entity.setUpdatedAt(LocalDateTime.of(2025, 2, 3, 4, 5));
        entity.setMajorVersion(4);
        entity.setMinorVersion(1);
        return entity;
    }

    private static UserEntity createUserEntity(String name, String email) {
        return new UserEntity(UUID.randomUUID(), name, email, "hash", "ADMIN",
                LocalDateTime.of(2025, 1, 2, 3, 4));
    }

    private static AdminEntity createInstructorEntity(String name) {
        return new AdminEntity(UUID.randomUUID(), name, "instructor@example.com",
                "hash", "ADMIN", LocalDateTime.of(2025, 1, 2, 3, 4), "Tourism");
    }

    private static CourseJpaRepository.ListingRow listingRow(
            UUID id, String title, String category, Double price,
            CourseStatus status, Integer majorVersion, Integer minorVersion) {
        return new CourseJpaRepository.ListingRow() {
            @Override
            public UUID getId() {
                return id;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public Double getPrice() {
                return price;
            }

            @Override
            public CourseStatus getStatus() {
                return status;
            }

            @Override
            public Integer getMajorVersion() {
                return majorVersion;
            }

            @Override
            public Integer getMinorVersion() {
                return minorVersion;
            }
        };
    }
}
