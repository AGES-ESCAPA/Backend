package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListCourseModulesUseCaseTest {

    private final InMemoryModuleRepositoryPort moduleRepository = new InMemoryModuleRepositoryPort();
    private final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
    private final CreateModuleUseCase createUseCase = new CreateModuleUseCase(moduleRepository, courseRepository);
    private final ListCourseModulesUseCase useCase = new ListCourseModulesUseCase(moduleRepository, courseRepository);
    private final UUID courseId = courseRepository.save(new Course()).getId();

    private static Content content(ContentType type, Integer durationMinutes) {
        final Content content = new Content();
        content.setId(UUID.randomUUID());
        content.setTitle("Aula");
        content.setType(type);
        content.setDurationMinutes(durationMinutes);
        return content;
    }

    @Test
    void shouldReturnModulesSortedByOrder() {
        final Module first = createUseCase.execute(courseId, "Modulo 1");
        final Module second = createUseCase.execute(courseId, "Modulo 2");
        final Module third = createUseCase.execute(courseId, "Modulo 3");
        // Simula uma ordem gravada fora de sequencia para provar que a listagem ordena.
        moduleRepository.reorder(courseId, List.of(third.getId(), first.getId(), second.getId()));

        final List<Module> modules = useCase.execute(courseId);

        assertEquals(List.of(third.getId(), first.getId(), second.getId()),
                modules.stream().map(Module::getId).toList());
        assertEquals(List.of(1, 2, 3), modules.stream().map(Module::getOrder).toList());
    }

    @Test
    void shouldAggregateTotalsFromContents() {
        final Module module = createUseCase.execute(courseId, "Modulo 1");
        module.getContents().add(content(ContentType.VIDEO, 90));
        module.getContents().add(content(ContentType.FILE, 60));
        module.getContents().add(content(ContentType.TEXT, null));

        final Module listed = useCase.execute(courseId).get(0);

        assertEquals(3, listed.getTotalContents());
        assertEquals(150, listed.getTotalDurationMinutes());
    }

    @Test
    void shouldReturnZeroTotalsForEmptyModule() {
        createUseCase.execute(courseId, "Modulo vazio");

        final Module listed = useCase.execute(courseId).get(0);

        assertEquals(0, listed.getTotalContents());
        assertEquals(0, listed.getTotalDurationMinutes());
        assertTrue(listed.getContents().isEmpty());
    }

    @Test
    void shouldReturnEmptyListForCourseWithoutModules() {
        assertTrue(useCase.execute(courseId).isEmpty());
    }

    @Test
    void shouldNotLeakModulesFromAnotherCourse() {
        final UUID otherCourseId = courseRepository.save(new Course()).getId();
        createUseCase.execute(courseId, "Meu modulo");
        createUseCase.execute(otherCourseId, "Modulo alheio");

        final List<Module> modules = useCase.execute(courseId);

        assertEquals(1, modules.size());
        assertEquals("Meu modulo", modules.get(0).getTitle());
    }

    @Test
    void shouldRejectUnknownCourse() {
        assertThrows(CourseNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }
}
