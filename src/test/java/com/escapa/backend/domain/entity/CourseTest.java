package com.escapa.backend.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.course.CourseQuizRules;
import com.escapa.backend.domain.course.CourseStatus;

class CourseTest {

    @Test
    void shouldApplyDefaultsWhenOptionalConstructorArgsAreNull() {
        final User instructor = new User("Instrutora", "instrutora@escapa.com", "hash", "ADMIN");
        final User creator = new User("Admin", "admin@escapa.com", "hash", "ADMIN");

        final Course course = new Course(
                "Titulo",
                "Resumo",
                "Descricao",
                null,
                null,
                instructor,
                "Hospedagem",
                "INICIANTE",
                null,
                null,
                null,
                null,
                null,
                null,
                creator);

        assertEquals(CourseStatus.DRAFT, course.getStatus());
        assertFalse(course.getFeatured());
        assertTrue(course.getRequireSequentialProgress());
        assertFalse(course.getEnforceDeadlineBlock());
        assertEquals(CourseQuizRules.DEFAULT_MIN_QUIZ_PASS_PERCENTAGE, course.getMinQuizPassPercentage());
        assertTrue(course.getLearningObjectives().isEmpty());
        assertNotNull(course.getCreatedAt());
        assertEquals(instructor, course.getInstructor());
        assertEquals(creator, course.getCreatedBy());
    }

    @Test
    void shouldKeepExplicitFeaturedAndObjectives() {
        final Course course = new Course(
                "Titulo",
                "Resumo",
                "Descricao",
                null,
                null,
                null,
                "Marketing",
                "AVANCADO",
                true,
                40,
                30,
                99.0,
                List.of("Objetivo"),
                false,
                true,
                new User("Admin", "admin@escapa.com", "hash", "ADMIN"));

        assertTrue(course.getFeatured());
        assertFalse(course.getRequireSequentialProgress());
        assertTrue(course.getEnforceDeadlineBlock());
        assertEquals(List.of("Objetivo"), course.getLearningObjectives());
        assertEquals(40, course.getDurationTime());
        assertEquals(99.0, course.getPrice());
    }
}
