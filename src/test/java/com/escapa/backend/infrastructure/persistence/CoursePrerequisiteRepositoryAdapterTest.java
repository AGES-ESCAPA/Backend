package com.escapa.backend.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;

@Transactional
class CoursePrerequisiteRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CoursePrerequisiteRepositoryPort prerequisites;

    @Autowired
    private CourseJpaRepository courses;

    @Test
    void shouldQuerySavedPrerequisite() {
        final CourseEntity course = saveCourse("Curso principal");
        final CourseEntity first = saveCourse("Primeiro requisito");

        prerequisites.save(course.getId(), first.getId());

        assertTrue(prerequisites.existsByCourseIdAndPrerequisiteCourseId(course.getId(), first.getId()));
        assertEquals(List.of(first.getId()), prerequisites.findPrerequisiteCourseIds(course.getId()));
    }

    @Test
    void shouldReplacePrerequisitesIgnoringNullAndSelfReference() {
        final CourseEntity course = saveCourse("Curso principal");
        final CourseEntity first = saveCourse("Primeiro requisito");
        final CourseEntity second = saveCourse("Segundo requisito");
        prerequisites.save(course.getId(), first.getId());

        prerequisites.replaceAll(course.getId(), Arrays.asList(second.getId(), null, course.getId()));

        assertEquals(List.of(second.getId()), prerequisites.findPrerequisiteCourseIds(course.getId()));
        assertFalse(prerequisites.existsByCourseIdAndPrerequisiteCourseId(course.getId(), first.getId()));
        assertTrue(prerequisites.existsByCourseIdAndPrerequisiteCourseId(course.getId(), second.getId()));
    }

    @Test
    void shouldDeletePrerequisite() {
        final CourseEntity course = saveCourse("Curso principal");
        final CourseEntity prerequisite = saveCourse("Requisito");
        prerequisites.save(course.getId(), prerequisite.getId());

        prerequisites.deleteByCourseIdAndPrerequisiteCourseId(course.getId(), prerequisite.getId());

        assertFalse(prerequisites.existsByCourseIdAndPrerequisiteCourseId(course.getId(), prerequisite.getId()));
    }

    @Test
    void shouldClearPrerequisitesWhenReplacementIsNull() {
        final CourseEntity course = saveCourse("Curso principal");
        final CourseEntity prerequisite = saveCourse("Requisito");
        prerequisites.save(course.getId(), prerequisite.getId());

        prerequisites.replaceAll(course.getId(), null);

        assertTrue(prerequisites.findPrerequisiteCourseIds(course.getId()).isEmpty());
    }

    private CourseEntity saveCourse(String title) {
        final CourseEntity course = new CourseEntity();
        course.setTitle(title);
        return courses.save(course);
    }
}
