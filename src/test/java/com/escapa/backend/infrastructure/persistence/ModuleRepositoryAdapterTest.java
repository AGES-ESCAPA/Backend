package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private ModuleRepositoryPort moduleRepositoryPort;

    @Autowired
    private ContentRepositoryPort contentRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private ContentJpaRepository contentJpaRepository;

    private UUID givenCourse() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Curso de teste");
        return courseJpaRepository.save(course).getId();
    }

    private Module givenModule(UUID courseId, String title) {
        final Module module = new Module();
        module.setCourseId(courseId);
        module.setTitle(title);
        module.setOrder(moduleRepositoryPort.nextOrder(courseId));
        return moduleRepositoryPort.save(module);
    }

    private Content givenContent(UUID moduleId, String title, Integer durationMinutes) {
        final Content content = new Content();
        content.setModuleId(moduleId);
        content.setTitle(title);
        content.setType(ContentType.VIDEO);
        content.setUrl("https://vimeo.com/123456789");
        content.setDurationMinutes(durationMinutes);
        content.setIsFree(false);
        content.setOrder(contentRepositoryPort.nextOrder(moduleId));
        return contentRepositoryPort.save(content);
    }

    @Test
    void shouldPersistAndRetrieveModule() {
        final UUID courseId = givenCourse();

        final Module saved = givenModule(courseId, "Modulo 1");

        assertNotNull(saved.getId());
        assertEquals(courseId, saved.getCourseId());
        assertEquals(1, saved.getOrder());
        assertTrue(moduleRepositoryPort.findById(saved.getId()).isPresent());
    }

    @Test
    void shouldCalculateNextOrderFromTheLastModule() {
        final UUID courseId = givenCourse();
        assertEquals(1, moduleRepositoryPort.nextOrder(courseId));

        givenModule(courseId, "Modulo 1");
        givenModule(courseId, "Modulo 2");

        assertEquals(3, moduleRepositoryPort.nextOrder(courseId));
    }

    @Test
    void shouldListModulesWithContentsSortedByOrder() {
        final UUID courseId = givenCourse();
        final Module first = givenModule(courseId, "Modulo 1");
        final Module second = givenModule(courseId, "Modulo 2");
        givenContent(first.getId(), "Aula 1", 90);
        givenContent(first.getId(), "Aula 2", 60);

        final List<Module> modules = moduleRepositoryPort.findByCourseId(courseId);

        assertEquals(List.of(first.getId(), second.getId()),
                modules.stream().map(Module::getId).toList());
        assertEquals(2, modules.get(0).getTotalContents());
        assertEquals(150, modules.get(0).getTotalDurationMinutes());
        assertEquals(List.of(1, 2),
                modules.get(0).getContents().stream().map(Content::getOrder).toList());
        assertEquals(0, modules.get(1).getTotalContents());
        assertTrue(modules.get(1).getContents().isEmpty());
    }

    @Test
    void shouldUpdateTitleKeepingOrder() {
        final UUID courseId = givenCourse();
        givenModule(courseId, "Modulo 1");
        final Module second = givenModule(courseId, "Modulo 2");

        second.setTitle("Modulo 2: CSS");
        final Module updated = moduleRepositoryPort.save(second);

        assertEquals("Modulo 2: CSS", updated.getTitle());
        assertEquals(2, updated.getOrder());
        assertEquals(courseId, updated.getCourseId());
    }

    @Test
    void shouldReorderWithoutViolatingTheUniqueOrderConstraint() {
        final UUID courseId = givenCourse();
        final Module first = givenModule(courseId, "Modulo 1");
        final Module second = givenModule(courseId, "Modulo 2");
        final Module third = givenModule(courseId, "Modulo 3");

        // Inverter a ordem cruza as posicoes: sem as duas fases, a primeira gravacao
        // ja colidiria com uk_modules_course_order.
        moduleRepositoryPort.reorder(courseId, List.of(third.getId(), second.getId(), first.getId()));

        final List<Module> reordered = moduleRepositoryPort.findByCourseId(courseId);
        assertEquals(List.of(third.getId(), second.getId(), first.getId()),
                reordered.stream().map(Module::getId).toList());
        assertEquals(List.of(1, 2, 3), reordered.stream().map(Module::getOrder).toList());
    }

    @Test
    void shouldSwapTwoAdjacentModules() {
        final UUID courseId = givenCourse();
        final Module first = givenModule(courseId, "Modulo 1");
        final Module second = givenModule(courseId, "Modulo 2");

        moduleRepositoryPort.reorder(courseId, List.of(second.getId(), first.getId()));

        final List<Module> reordered = moduleRepositoryPort.findByCourseId(courseId);
        assertEquals(List.of(second.getId(), first.getId()),
                reordered.stream().map(Module::getId).toList());
    }

    @Test
    void shouldNotTouchModulesOfAnotherCourseOnReorder() {
        final UUID courseId = givenCourse();
        final UUID otherCourseId = givenCourse();
        final Module mine = givenModule(courseId, "Meu modulo");
        final Module foreign = givenModule(otherCourseId, "Modulo alheio");

        // O filtro por course_id no UPDATE ignora o id de fora; o modulo alheio fica intacto.
        moduleRepositoryPort.reorder(courseId, List.of(foreign.getId(), mine.getId()));

        assertEquals(1, moduleRepositoryPort.findById(foreign.getId()).orElseThrow().getOrder());
        assertEquals(2, moduleRepositoryPort.findById(mine.getId()).orElseThrow().getOrder());
    }

    @Test
    void shouldDeleteModuleCascadingItsContents() {
        final UUID courseId = givenCourse();
        final Module module = givenModule(courseId, "Modulo 1");
        final Content content = givenContent(module.getId(), "Aula 1", 10);

        moduleRepositoryPort.deleteById(module.getId());

        assertTrue(moduleRepositoryPort.findById(module.getId()).isEmpty());
        assertTrue(contentJpaRepository.findById(content.getId()).isEmpty());
    }
}
