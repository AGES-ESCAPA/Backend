package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.CourseDetails;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CourseDetailsResponseTest {

    @Test
    void shouldMapInstructorMaterialsModulesAndNestedContents() {
        final UUID courseId = UUID.randomUUID();
        final UUID instructorId = UUID.randomUUID();
        final UUID moduleId = UUID.randomUUID();
        final UUID contentId = UUID.randomUUID();
        final CourseDetails course = new CourseDetails(
                courseId, "Curso", "Resumo", "Descrição", "Turismo", "Iniciante", 10, 99.0, 30,
                "https://example.com/cover.jpg", "https://example.com/intro.mp4", 4.5, 12, 20,
                new CourseDetails.Instructor(instructorId, "Ana", "Guia", "Bio", "https://example.com/ana.jpg"),
                List.of("Aprender"), List.of(new CourseDetails.Material("Guia", "PDF", "https://example.com/guia")),
                List.of(new CourseDetails.Module(
                        moduleId, "Módulo", 1, 1, 15,
                        List.of(new CourseDetails.Content(contentId, "Aula", "VIDEO", 1, 15, true, "video-url")))));

        final CourseDetailsResponse response = CourseDetailsResponse.from(course);

        assertEquals(courseId, response.id());
        assertEquals("https://example.com/intro.mp4", response.teaserVideoUrl());
        assertEquals(new CourseDetailsResponse.InstructorResponse(
                instructorId, "Ana", "Guia", "Bio", "https://example.com/ana.jpg"), response.instructor());
        assertEquals(new CourseDetailsResponse.MaterialResponse("Guia", "PDF", "https://example.com/guia"),
                response.materials().get(0));
        final CourseDetailsResponse.ModuleResponse module = response.modules().get(0);
        assertEquals(moduleId, module.id());
        assertEquals(15, module.durationMinutes());
        assertEquals(new CourseDetailsResponse.ContentResponse(
                contentId, "Aula", "VIDEO", 1, 15, true, "video-url"), module.contents().get(0));
    }

    @Test
    void shouldPreserveAbsentInstructorAndEmptyCollections() {
        final CourseDetails course = new CourseDetails(
                UUID.randomUUID(), "Curso", null, null, null, null, null, null, null, null, null, null, 0, 0,
                null, List.of(), List.of(), List.of());

        final CourseDetailsResponse response = CourseDetailsResponse.from(course);

        assertNull(response.instructor());
        assertEquals(List.of(), response.materials());
        assertEquals(List.of(), response.modules());
    }
}
