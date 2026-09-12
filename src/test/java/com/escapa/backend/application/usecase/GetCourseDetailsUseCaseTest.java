package com.escapa.backend.application.usecase;

import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetCourseDetailsUseCaseTest {

    @Test
    void shouldReturnCourseWhenFound() {
        final InMemoryCourseRepositoryPort repository = new InMemoryCourseRepositoryPort();
        final CourseDetails course = sampleCourse();
        repository.saveDetails(course);
        final GetCourseDetailsUseCase useCase = new GetCourseDetailsUseCase(repository);

        final CourseDetails found = useCase.execute(course.id());

        assertEquals(course.id(), found.id());
        assertEquals(course.title(), found.title());
        assertEquals(1, found.modules().size());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final CourseRepositoryPort repository = new InMemoryCourseRepositoryPort();
        final GetCourseDetailsUseCase useCase = new GetCourseDetailsUseCase(repository);

        assertThrows(CourseNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }

    private static CourseDetails sampleCourse() {
        final CourseDetails.Content freeContent = new CourseDetails.Content(
                UUID.randomUUID(), "Introducao", "VIDEO", 1, 10, true, "https://cdn.example.com/1.mp4"
        );
        final CourseDetails.Content paidContent = new CourseDetails.Content(
                UUID.randomUUID(), "Aula avancada", "VIDEO", 2, 20, false, null
        );
        final CourseDetails.Module module = new CourseDetails.Module(
                UUID.randomUUID(), "Modulo 1", 1, 2, 30, List.of(freeContent, paidContent)
        );
        final CourseDetails.Instructor instructor = new CourseDetails.Instructor(
                UUID.randomUUID(), "Dra. Mariana Fonseca", "Pesquisadora", "Bio da instrutora"
        );

        return new CourseDetails(
                UUID.randomUUID(),
                "IA Aplicada ao Turismo",
                "Resumo curto",
                "Descricao completa",
                "Inteligencia Artificial",
                "Iniciante",
                12,
                97.00,
                365,
                "https://cdn.example.com/thumb.jpg",
                4.9,
                247,
                1840,
                instructor,
                List.of("Objetivo 1", "Objetivo 2"),
                List.of(),
                List.of(module)
        );
    }
}
