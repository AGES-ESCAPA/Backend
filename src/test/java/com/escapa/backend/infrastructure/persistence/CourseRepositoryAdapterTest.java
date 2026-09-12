package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.AdminEntity;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseMaterialEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import com.escapa.backend.infrastructure.persistence.entity.enums.ContentType;
import com.escapa.backend.infrastructure.persistence.entity.enums.CourseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o caminho real de persistencia do endpoint publico de detalhes do
 * curso (US-04), incluindo o ponto que motivou a divisao de
 * {@code CourseJpaRepository#findWithModulesById} em consultas separadas:
 * buscar "modules", "materials" e "modules.contents" (todas colecoes List
 * sem indice, ou seja, "bags") em uma unica consulta com fetch join faz o
 * Hibernate lancar {@code MultipleBagFetchException}.
 */
class CourseRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CourseRepositoryPort courseRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void shouldReturnPublishedCourseWithModulesMaterialsAndHiddenPaidUrls() {
        final CourseEntity course = persistCourseWithFullGraph(CourseStatus.PUBLISHED);

        final Optional<CourseDetails> found = courseRepositoryPort.findDetailsById(course.getId());

        assertTrue(found.isPresent());
        final CourseDetails details = found.get();
        assertEquals(course.getTitle(), details.title());
        assertEquals(1, details.materials().size());
        assertEquals(1, details.modules().size());

        final CourseDetails.Module module = details.modules().get(0);
        assertEquals(2, module.totalContents());
        assertEquals(30, module.durationMinutes());
        assertEquals(2, module.contents().size());

        final CourseDetails.Content freeContent = module.contents().get(0);
        final CourseDetails.Content paidContent = module.contents().get(1);
        assertTrue(freeContent.isFree());
        assertEquals("https://cdn.example.com/free.mp4", freeContent.url());
        assertFalse(paidContent.isFree());
        assertNull(paidContent.url());
    }

    @Test
    void shouldReturnEmptyWhenCourseIsNotPublished() {
        final CourseEntity course = persistCourseWithFullGraph(CourseStatus.DRAFT);

        final Optional<CourseDetails> found = courseRepositoryPort.findDetailsById(course.getId());

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenCourseDoesNotExist() {
        final Optional<CourseDetails> found = courseRepositoryPort.findDetailsById(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    private CourseEntity persistCourseWithFullGraph(CourseStatus status) {
        final AdminEntity instructor = new AdminEntity(
                UUID.randomUUID(),
                "Dra. Mariana Fonseca",
                "mariana." + UUID.randomUUID() + "@escapa.com.br",
                "hash",
                "ADMIN",
                LocalDateTime.now(),
                "Turismo"
        );
        instructor.setHeadline("Pesquisadora em Turismo & IA");
        instructor.setBio("Bio da instrutora");
        userJpaRepository.saveAndFlush(instructor);

        final CourseEntity course = new CourseEntity();
        course.setTitle("IA Aplicada ao Turismo");
        course.setShortDescription("Resumo curto");
        course.setDescription("Descricao completa");
        course.setStatus(status);
        course.setInstructor(instructor);
        course.setCategory("Inteligencia Artificial");
        course.setLevel("Iniciante");
        course.setDurationTime(12);
        course.setPrice(97.00);
        course.setDeadline(365);

        final CourseMaterialEntity material = new CourseMaterialEntity();
        material.setCourse(course);
        material.setTitle("Guia de Prompts");
        material.setFileUrl("https://cdn.example.com/guia.pdf");
        material.setFileType("PDF");
        material.setOrder(1);
        course.setMaterials(List.of(material));

        final ModuleEntity module = new ModuleEntity();
        module.setCourse(course);
        module.setTitle("Modulo 1");
        module.setOrder(1);

        final ContentEntity freeContent = new ContentEntity();
        freeContent.setModule(module);
        freeContent.setTitle("Introducao");
        freeContent.setType(ContentType.VIDEO);
        freeContent.setUrl("https://cdn.example.com/free.mp4");
        freeContent.setDurationMinutes(10);
        freeContent.setIsFree(true);
        freeContent.setOrder(1);

        final ContentEntity paidContent = new ContentEntity();
        paidContent.setModule(module);
        paidContent.setTitle("Aula avancada");
        paidContent.setType(ContentType.VIDEO);
        paidContent.setUrl("https://cdn.example.com/paid.mp4");
        paidContent.setDurationMinutes(20);
        paidContent.setIsFree(false);
        paidContent.setOrder(2);

        module.setContents(List.of(freeContent, paidContent));
        course.setModules(List.of(module));

        return courseJpaRepository.saveAndFlush(course);
    }
}
