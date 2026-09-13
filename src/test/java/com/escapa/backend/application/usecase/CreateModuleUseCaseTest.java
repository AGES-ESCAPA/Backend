package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateModuleUseCaseTest {

    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
    private final CreateModuleUseCase useCase = new CreateModuleUseCase(moduleRepository, courseRepository);
    private final UUID courseId = courseRepository.save(new Course()).getId();

    @Test
    void shouldCreateModuleAtTheEndOfTheCourse() {
        final Module module = useCase.execute(courseId, "  Modulo 1: Fundamentos  ");

        assertNotNull(module.getId());
        assertEquals("Modulo 1: Fundamentos", module.getTitle());
        assertEquals(courseId, module.getCourseId());
        assertEquals(1, module.getOrder());
        assertTrue(module.getContents().isEmpty());
    }

    @Test
    void shouldIncrementOrderFollowingTheLastModuleOfTheCourse() {
        useCase.execute(courseId, "Modulo 1");
        useCase.execute(courseId, "Modulo 2");
        final Module third = useCase.execute(courseId, "Modulo 3");

        assertEquals(3, third.getOrder());
    }

    @Test
    void shouldNotMixOrderBetweenCourses() {
        final UUID otherCourseId = courseRepository.save(new Course()).getId();
        useCase.execute(courseId, "Modulo 1");
        useCase.execute(courseId, "Modulo 2");

        final Module first = useCase.execute(otherCourseId, "Outro curso, modulo 1");

        assertEquals(1, first.getOrder());
    }

    @Test
    void shouldRejectUnknownCourse() {
        assertThrows(
                CourseNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), "Modulo")
        );
    }

    @Test
    void shouldRejectBlankTitle() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(courseId, "   "));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(courseId, null));
    }

    @Test
    void shouldRejectNullCourseId() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, "Modulo"));
    }
}
