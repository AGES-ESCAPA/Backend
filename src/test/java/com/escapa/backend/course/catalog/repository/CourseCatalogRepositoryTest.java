package com.escapa.backend.course.catalog.repository;

import com.escapa.backend.common.JpaIntegrationTest;
import com.escapa.backend.course.catalog.dto.CourseCardResponse;
import com.escapa.backend.course.shared.entity.CourseEntity;
import com.escapa.backend.course.shared.entity.CourseStatus;
import com.escapa.backend.user.entity.AdminEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A consulta JPQL contra o Postgres real: status, filtros, escape, ordem e paginação.
 * Roda dentro de uma transação desfeita ao final; o setUp esvazia courses para não depender
 * da ordem em que as classes de teste rodam.
 */
class CourseCatalogRepositoryTest extends JpaIntegrationTest {

    private static final String ALL = "%";
    private static final String ANY = "";

    @Autowired
    private CourseCatalogRepository repository;

    @Autowired
    private TestEntityManager em;

    private AdminEntity instructor;

    @BeforeEach
    void setUp() {
        // Testes de controller (@SpringBootTest) nao sao transacionais e deixam cursos no mesmo banco.
        // Limpa dentro desta transacao, que e desfeita ao final; o cascade do banco remove dependentes.
        em.getEntityManager().createNativeQuery("DELETE FROM courses").executeUpdate();
        instructor = em.persist(new AdminEntity(UUID.randomUUID(), "Dra. Mariana",
                "mariana." + UUID.randomUUID().toString().substring(0, 8) + "@escapa.com",
                "hash", "ADMIN", LocalDateTime.now(), "Turismo"));
    }

    @Test
    void shouldReturnOnlyPublishedCourses() {
        publish("Curso Publicado", "Turismo", "INICIANTE");
        course("Curso Rascunho", "Turismo", "INICIANTE", CourseStatus.DRAFT);
        course("Curso Arquivado", "Turismo", "INICIANTE", CourseStatus.ARCHIVED);

        final Page<CourseCardResponse> page = repository.findPublished(ALL, ANY, ANY, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Curso Publicado", page.getContent().get(0).title());
    }

    @Test
    void shouldFilterByPartialTitleIgnoringCase() {
        publish("Atendimento de Excelência", "Hospitalidade", "INICIANTE");
        publish("Gestão de Reservas", "Turismo", "INTERMEDIARIO");

        final Page<CourseCardResponse> page = repository.findPublished("%atendimento%", ANY, ANY, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("Atendimento de Excelência", page.getContent().get(0).title());
    }

    @Test
    void shouldFilterByCategoryAndLevelIgnoringCase() {
        publish("Curso A", "Hospitalidade", "INICIANTE");
        publish("Curso B", "Turismo", "INICIANTE");
        publish("Curso C", "Hospitalidade", "AVANCADO");

        final Page<CourseCardResponse> byCategory = repository.findPublished(ALL, "hospitalidade", ANY, PageRequest.of(0, 10));
        final Page<CourseCardResponse> byLevel = repository.findPublished(ALL, ANY, "iniciante", PageRequest.of(0, 10));
        final Page<CourseCardResponse> both = repository.findPublished(ALL, "hospitalidade", "iniciante", PageRequest.of(0, 10));

        assertEquals(2, byCategory.getTotalElements());
        assertEquals(2, byLevel.getTotalElements());
        assertEquals(1, both.getTotalElements());
        assertEquals("Curso A", both.getContent().get(0).title());
    }

    @Test
    void shouldTreatEscapedWildcardsAsLiteralText() {
        publish("Curso 100% Prático", "Turismo", "INICIANTE");
        publish("Curso Comum", "Turismo", "INICIANTE");

        final Page<CourseCardResponse> literalPercent = repository.findPublished("%100!%%", ANY, ANY, PageRequest.of(0, 10));
        final Page<CourseCardResponse> literalUnderscore = repository.findPublished("%!_%", ANY, ANY, PageRequest.of(0, 10));

        assertEquals(1, literalPercent.getTotalElements());
        assertEquals("Curso 100% Prático", literalPercent.getContent().get(0).title());
        assertEquals(0, literalUnderscore.getTotalElements());
    }

    @Test
    void shouldOrderNewestFirstAndKeepOrderStableAcrossPages() {
        publishAt("Antigo", LocalDateTime.of(2026, 1, 1, 10, 0));
        publishAt("Recente", LocalDateTime.of(2026, 3, 1, 10, 0));
        publishAt("Médio", LocalDateTime.of(2026, 2, 1, 10, 0));

        final Page<CourseCardResponse> first = repository.findPublished(ALL, ANY, ANY, PageRequest.of(0, 2));
        final Page<CourseCardResponse> second = repository.findPublished(ALL, ANY, ANY, PageRequest.of(1, 2));

        assertEquals(List.of("Recente", "Médio"), first.getContent().stream().map(CourseCardResponse::title).toList());
        assertEquals(List.of("Antigo"), second.getContent().stream().map(CourseCardResponse::title).toList());
        assertEquals(3, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
    }

    @Test
    void shouldProjectInstructorNameAndDenormalizedCounters() {
        final CourseEntity course = publish("Curso Completo", "Turismo", "INICIANTE");
        course.setLessonsCount(5);
        course.setReviewsCount(10);
        course.setRatingAverage(4.5);
        course.setPrice(97.0);
        course.setDurationTime(12);
        course.setThumbnailUrl("https://cdn.escapa.com.br/thumb.jpg");
        em.flush();

        final CourseCardResponse card = repository.findPublished(ALL, ANY, ANY, PageRequest.of(0, 10))
                .getContent().get(0);

        assertEquals(course.getId(), card.id());
        assertEquals("Dra. Mariana", card.instructor());
        assertEquals(5, card.lessonsCount());
        assertEquals(10, card.reviewsCount());
        assertEquals(4.5, card.ratingAverage());
        assertEquals(97.0, card.price());
        assertEquals(12, card.durationTime());
        assertEquals("https://cdn.escapa.com.br/thumb.jpg", card.thumbnailUrl());
        assertEquals("Descrição de Curso Completo", card.shortDescription());
    }

    @Test
    void shouldReturnEmptyPageWhenNothingMatches() {
        final Page<CourseCardResponse> page = repository.findPublished(ALL, "inexistente", ANY, PageRequest.of(0, 10));

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getTotalPages());
    }

    private CourseEntity publish(String title, String category, String level) {
        return course(title, category, level, CourseStatus.PUBLISHED);
    }

    private void publishAt(String title, LocalDateTime createdAt) {
        final CourseEntity course = course(title, "Turismo", "INICIANTE", CourseStatus.PUBLISHED);
        course.setCreatedAt(createdAt);
        em.flush();
    }

    private CourseEntity course(String title, String category, String level, CourseStatus status) {
        final CourseEntity course = new CourseEntity();
        course.setTitle(title);
        course.setShortDescription("Descrição de " + title);
        course.setCategory(category);
        course.setLevel(level);
        course.setStatus(status);
        course.setInstructor(instructor);
        course.setCreatedBy(instructor);
        course.setCreatedAt(LocalDateTime.now());
        course.setPrice(99.90);
        return em.persist(course);
    }
}
