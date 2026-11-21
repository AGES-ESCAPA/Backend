package com.escapa.backend.infrastructure.persistence.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;

class CourseMapperTest {

    @Test
    void shouldReturnNullWhenMappingNullInputs() {
        assertNull(CourseMapper.toSummary(null));
        assertNull(CourseMapper.toDomain(null));
    }

    @Test
    void shouldKeepExistingValuesWhenApplyInputsAreNull() {
        final CourseEntity entity = new CourseEntity();
        entity.setTitle("Existing title");
        final Course course = new Course();
        course.setTitle("Domain title");

        CourseMapper.applyToEntity(null, entity);
        CourseMapper.applyToEntity(course, null);

        assertEquals("Existing title", entity.getTitle());
        assertEquals("Domain title", course.getTitle());
    }

    @Test
    void shouldMapSummaryAndAdminListingFields() {
        final UUID courseId = UUID.randomUUID();
        final CourseEntity entity = new CourseEntity();
        entity.setId(courseId);
        entity.setTitle("Curso");
        entity.setShortDescription("Resumo");
        entity.setCategory("Turismo");
        entity.setLevel("Iniciante");
        entity.setDurationTime(12);
        entity.setPrice(75.0);
        entity.setRatingAverage(4.5);
        entity.setReviewsCount(8);

        final var summary = CourseMapper.toSummary(entity);
        final Course listing = CourseMapper.toListingDomain(new CourseJpaRepository.ListingRow() {
            @Override
            public UUID getId() {
                return courseId;
            }

            @Override
            public String getTitle() {
                return "Curso";
            }

            @Override
            public String getCategory() {
                return "Turismo";
            }

            @Override
            public Double getPrice() {
                return 75.0;
            }

            @Override
            public CourseStatus getStatus() {
                return CourseStatus.PUBLISHED;
            }

            @Override
            public Integer getMajorVersion() {
                return 2;
            }

            @Override
            public Integer getMinorVersion() {
                return 3;
            }
        });

        assertEquals(courseId, summary.id());
        assertNull(summary.instructorName());
        assertEquals("Resumo", summary.shortDescription());
        assertEquals(8, summary.reviewsCount());
        assertEquals(CourseStatus.PUBLISHED, listing.getStatus());
        assertEquals(2, listing.getMajorVersion());
        assertEquals(3, listing.getMinorVersion());
    }

    @Test
    void shouldMapEmptyAndMalformedLearningObjectivesAndEmptyModules() {
        final CourseEntity entity = new CourseEntity();
        entity.setLearningObjectives(" ");
        final ModuleEntity module = new ModuleEntity();
        module.setId(UUID.randomUUID());
        module.setCourse(entity);
        module.setTitle("Módulo");
        module.setOrder(1);
        entity.setModules(List.of(module));

        final Course courseWithEmptyObjectives = CourseMapper.toDomain(entity);

        assertEquals(List.of(), courseWithEmptyObjectives.getLearningObjectives());
        assertEquals(1, courseWithEmptyObjectives.getModules().size());
        assertEquals("Módulo", courseWithEmptyObjectives.getModules().get(0).getTitle());

        entity.setLearningObjectives("{invalid");
        final Course courseWithMalformedObjectives = CourseMapper.toDomain(entity);

        assertEquals(List.of(), courseWithMalformedObjectives.getLearningObjectives());
    }

    @Test
    void shouldRejectMalformedPendingDraftJson() {
        final CourseEntity entity = new CourseEntity();
        entity.setPendingDraft("{invalid");

        final IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> CourseMapper.toDomain(entity));

        assertEquals("Invalid course draft JSON", exception.getMessage());
    }

    @Test
    void shouldLeaveExistingObjectivesUntouchedWhenDomainOmitsThem() {
        final Course course = new Course();
        course.setStatus(null);
        course.setLearningObjectives(null);
        final CourseEntity entity = new CourseEntity();
        entity.setLearningObjectives("[\"existing\"]");
        entity.setStatus(CourseStatus.PUBLISHED);

        CourseMapper.applyToEntity(course, entity);

        assertEquals("[\"existing\"]", entity.getLearningObjectives());
        assertEquals(CourseStatus.PUBLISHED, entity.getStatus());
        assertNull(entity.getPendingDraft());
    }

    @Test
    void shouldPreserveDefaultsWhenStatusModulesOrContentsAreNull() {
        final CourseEntity entity = new CourseEntity();
        entity.setStatus(null);
        entity.setModules(null);
        final ModuleEntity module = new ModuleEntity();
        module.setContents(null);
        entity.setModules(List.of(module));

        final Course course = CourseMapper.toDomain(entity);

        assertEquals(CourseStatus.DRAFT, course.getStatus());
        assertEquals(1, course.getModules().size());
        assertTrue(course.getModules().get(0).getContents().isEmpty());
    }
}
