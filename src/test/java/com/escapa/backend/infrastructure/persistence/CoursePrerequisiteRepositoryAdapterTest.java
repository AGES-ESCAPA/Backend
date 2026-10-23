package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class CoursePrerequisiteRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CoursePrerequisiteRepositoryPort prerequisiteRepository;

    @Autowired
    private CourseJpaRepository courseRepository;

    @Test
    void shouldSaveFindCheckAndDeleteCoursePrerequisite() {
        final CourseEntity course = saveCourse("Curso avançado");
        final CourseEntity prerequisite = saveCourse("Curso básico");

        assertFalse(prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                course.getId(), prerequisite.getId()));
        prerequisiteRepository.save(course.getId(), prerequisite.getId());

        assertEquals(List.of(prerequisite.getId()),
                prerequisiteRepository.findPrerequisiteCourseIds(course.getId()));
        assertTrue(prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                course.getId(), prerequisite.getId()));
        assertFalse(prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                prerequisite.getId(), course.getId()));

        prerequisiteRepository.deleteByCourseIdAndPrerequisiteCourseId(course.getId(), prerequisite.getId());

        assertTrue(prerequisiteRepository.findPrerequisiteCourseIds(course.getId()).isEmpty());
        assertFalse(prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                course.getId(), prerequisite.getId()));
    }

    private CourseEntity saveCourse(String title) {
        final CourseEntity course = new CourseEntity();
        course.setTitle(title);
        course.setStatus(CourseStatus.DRAFT);
        return courseRepository.saveAndFlush(course);
    }
}
