package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class CoursePrerequisiteRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CoursePrerequisiteRepositoryPort prerequisites;

    @Autowired
    private CourseJpaRepository courses;

    @Test
    void shouldReplaceDeleteAndQueryPrerequisites() {
        final CourseEntity course = saveCourse("Curso principal");
        final CourseEntity first = saveCourse("Primeiro requisito");
        final CourseEntity second = saveCourse("Segundo requisito");

        prerequisites.save(course.getId(), first.getId());

        assertTrue(prerequisites.existsByCourseIdAndPrerequisiteCourseId(course.getId(), first.getId()));
        assertEquals(List.of(first.getId()), prerequisites.findPrerequisiteCourseIds(course.getId()));

        prerequisites.replaceAll(course.getId(), Arrays.asList(second.getId(), null, course.getId()));

        assertEquals(List.of(second.getId()), prerequisites.findPrerequisiteCourseIds(course.getId()));
        assertTrue(prerequisites.existsByCourseIdAndPrerequisiteCourseId(course.getId(), second.getId()));

        prerequisites.deleteByCourseIdAndPrerequisiteCourseId(course.getId(), second.getId());

        assertFalse(prerequisites.existsByCourseIdAndPrerequisiteCourseId(course.getId(), second.getId()));
        prerequisites.replaceAll(course.getId(), null);
        assertTrue(prerequisites.findPrerequisiteCourseIds(course.getId()).isEmpty());
    }

    private CourseEntity saveCourse(String title) {
        final CourseEntity course = new CourseEntity();
        course.setTitle(title);
        return courses.save(course);
    }
}
