package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private ContentRepositoryPort contentRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private ModuleJpaRepository moduleJpaRepository;

    private UUID givenModule() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Curso de teste");
        final CourseEntity savedCourse = courseJpaRepository.save(course);

        final ModuleEntity module = new ModuleEntity();
        module.setCourse(savedCourse);
        module.setTitle("Modulo de teste");
        module.setOrder(1);
        return moduleJpaRepository.save(module).getId();
    }

    private Content givenContent(UUID moduleId, String title) {
        final Content content = new Content();
        content.setModuleId(moduleId);
        content.setTitle(title);
        content.setType(ContentType.TEXT);
        content.setDescription("Texto");
        content.setIsFree(false);
        content.setOrder(contentRepositoryPort.nextOrder(moduleId));
        return contentRepositoryPort.save(content);
    }

    @Test
    void shouldPersistAndRetrieveContent() {
        final UUID moduleId = givenModule();

        final Content saved = givenContent(moduleId, "Aula 1");

        assertNotNull(saved.getId());
        assertEquals(moduleId, saved.getModuleId());
        assertTrue(contentRepositoryPort.findById(saved.getId()).isPresent());
    }

    @Test
    void shouldCalculateNextOrderFromTheLastContent() {
        final UUID moduleId = givenModule();
        assertEquals(1, contentRepositoryPort.nextOrder(moduleId));

        givenContent(moduleId, "Aula 1");
        givenContent(moduleId, "Aula 2");

        assertEquals(3, contentRepositoryPort.nextOrder(moduleId));
    }

    @Test
    void shouldReorderWithoutViolatingTheUniqueOrderConstraint() {
        final UUID moduleId = givenModule();
        final Content first = givenContent(moduleId, "Aula 1");
        final Content second = givenContent(moduleId, "Aula 2");
        final Content third = givenContent(moduleId, "Aula 3");

        // Inverter a ordem cruza as posicoes: sem as duas fases, a primeira gravacao
        // ja colidiria com uk_content_module_order.
        contentRepositoryPort.reorder(moduleId, List.of(third.getId(), second.getId(), first.getId()));

        final List<Content> reordered = contentRepositoryPort.findByModuleId(moduleId);
        assertEquals(List.of(third.getId(), second.getId(), first.getId()),
                reordered.stream().map(Content::getId).toList());
        assertEquals(List.of(1, 2, 3), reordered.stream().map(Content::getOrder).toList());
    }

    @Test
    void shouldSwapTwoAdjacentContents() {
        final UUID moduleId = givenModule();
        final Content first = givenContent(moduleId, "Aula 1");
        final Content second = givenContent(moduleId, "Aula 2");

        contentRepositoryPort.reorder(moduleId, List.of(second.getId(), first.getId()));

        final List<Content> reordered = contentRepositoryPort.findByModuleId(moduleId);
        assertEquals(List.of(second.getId(), first.getId()),
                reordered.stream().map(Content::getId).toList());
    }

    @Test
    void shouldDeleteContent() {
        final UUID moduleId = givenModule();
        final Content content = givenContent(moduleId, "Aula 1");

        contentRepositoryPort.deleteById(content.getId());

        assertTrue(contentRepositoryPort.findById(content.getId()).isEmpty());
    }
}
