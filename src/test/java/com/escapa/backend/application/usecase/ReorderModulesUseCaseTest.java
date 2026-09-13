package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReorderModulesUseCaseTest {

    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
    private final CreateModuleUseCase createUseCase = new CreateModuleUseCase(moduleRepository, courseRepository);
    private final ReorderModulesUseCase useCase = new ReorderModulesUseCase(moduleRepository, courseRepository);
    private final UUID courseId = courseRepository.save(new Course()).getId();

    private Module givenModule(String title) {
        return createUseCase.execute(courseId, title);
    }

    @Test
    void shouldRewriteOrderFollowingTheRequestedSequence() {
        final Module first = givenModule("Modulo 1");
        final Module second = givenModule("Modulo 2");
        final Module third = givenModule("Modulo 3");

        final List<Module> reordered = useCase.execute(
                courseId, List.of(second.getId(), first.getId(), third.getId()));

        assertEquals(List.of(second.getId(), first.getId(), third.getId()),
                reordered.stream().map(Module::getId).toList());
        assertEquals(List.of(1, 2, 3), reordered.stream().map(Module::getOrder).toList());
    }

    @Test
    void shouldRejectPartialSequence() {
        final Module first = givenModule("Modulo 1");
        givenModule("Modulo 2");

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(courseId, List.of(first.getId()))
        );
    }

    @Test
    void shouldRejectDuplicatedIds() {
        final Module first = givenModule("Modulo 1");
        givenModule("Modulo 2");

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(courseId, List.of(first.getId(), first.getId()))
        );
    }

    @Test
    void shouldRejectModuleFromAnotherCourse() {
        final Module first = givenModule("Modulo 1");
        final UUID otherCourseId = courseRepository.save(new Course()).getId();
        final Module foreign = createUseCase.execute(otherCourseId, "Modulo alheio");

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(courseId, List.of(first.getId(), foreign.getId()))
        );
        assertEquals(1, moduleRepository.findById(foreign.getId()).orElseThrow().getOrder());
    }

    @Test
    void shouldRejectUnknownId() {
        final Module first = givenModule("Modulo 1");

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(courseId, List.of(first.getId(), UUID.randomUUID()))
        );
    }

    @Test
    void shouldRejectEmptyList() {
        givenModule("Modulo 1");

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(courseId, List.of()));
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(courseId, null));
    }

    @Test
    void shouldRejectUnknownCourse() {
        assertThrows(
                CourseNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), List.of(UUID.randomUUID()))
        );
    }
}
