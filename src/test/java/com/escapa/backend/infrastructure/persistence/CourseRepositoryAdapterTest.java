package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.infrastructure.persistence.entity.AdminEntity;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseMaterialEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre a listagem pública paginada de cursos (US-01) e os detalhes
 * completos de um curso (US-04), incluindo o ponto que motivou a divisao de
 * {@code CourseJpaRepository#findWithModulesById} em consultas separadas:
 * buscar "modules", "materials" e "modules.contents" (todas colecoes List
 * sem indice, ou seja, "bags") em uma unica consulta com fetch join faz o
 * Hibernate lancar {@code MultipleBagFetchException}.
 */
@Transactional
class CourseRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CourseRepositoryPort courseRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void shouldPersistNewCourseFromDomainAndReturnMappedFields() {
        final Course course = new Course("Curso novo", "Resumo", "Descrição",
                "/thumb.png", null, null, "Turismo", "INICIANTE",
                60, 30, 49.90, List.of("Objetivo"), false, true, null);

        final Course saved = courseRepositoryPort.save(course);

        assertNotNull(saved.getId());
        assertEquals("Curso novo", saved.getTitle());
        assertEquals(CourseStatus.DRAFT, saved.getStatus());
        assertEquals("Turismo", saved.getCategory());
        assertEquals(List.of("Objetivo"), saved.getLearningObjectives());
        final Course persisted = courseRepositoryPort.findById(saved.getId()).orElseThrow();
        assertEquals(saved.getId(), persisted.getId());
        assertEquals("Descrição", persisted.getDescription());
        assertEquals(49.90, persisted.getPrice());
    }

    @BeforeEach
    void cleanUp() {
        courseJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void shouldReturnOnlyPublishedCourses() {
        final AdminEntity instructor = createInstructor("Instrutora Publicada");
        createCourse("Curso Publicado", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso Rascunho", "Turismo", "INICIANTE", CourseStatus.DRAFT, instructor);
        createCourse("Curso Arquivado", "Turismo", "INICIANTE", CourseStatus.ARCHIVED, instructor);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, null, null, 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("Curso Publicado", result.content().get(0).title());
    }

    @Test
    void shouldListAdminCoursesWithoutArchivedOnesAndWithoutLoadingModules() {
        final AdminEntity instructor = createInstructor("Instrutora Listagem");
        createCourse("Curso Publicado", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso Rascunho", "Hotelaria", "INICIANTE", CourseStatus.DRAFT, instructor);
        createCourse("Curso Arquivado", "Turismo", "INICIANTE", CourseStatus.ARCHIVED, instructor);

        final List<Course> courses = courseRepositoryPort.findAllExcludingStatus(CourseStatus.ARCHIVED);

        assertEquals(
                Set.of("Curso Publicado", "Curso Rascunho"),
                courses.stream().map(Course::getTitle).collect(Collectors.toSet()));
        final Course published = courses.stream()
                .filter(course -> course.getStatus() == CourseStatus.PUBLISHED)
                .findFirst()
                .orElseThrow();
        assertEquals("Turismo", published.getCategory());
        assertEquals(99.90, published.getPrice());
        assertEquals(0, published.getMajorVersion());
        assertTrue(published.getModules().isEmpty());
    }

    @Test
    void shouldFilterByTitleCaseInsensitive() {
        final AdminEntity instructor = createInstructor("Instrutora Titulo");
        createCourse("Atendimento de Excelência", "Hospitalidade", "INICIANTE",
                CourseStatus.PUBLISHED, instructor);
        createCourse("Gestão de Reservas", "Turismo", "INTERMEDIÁRIO",
                CourseStatus.PUBLISHED, instructor);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                "atendimento", null, null, 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("Atendimento de Excelência", result.content().get(0).title());
    }

    @Test
    void shouldFilterByCategory() {
        final AdminEntity instructor = createInstructor("Instrutora Categoria");
        createCourse("Curso A", "Hospitalidade", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso B", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, "Hospitalidade", null, 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("Curso A", result.content().get(0).title());
    }

    @Test
    void shouldFilterByLevel() {
        final AdminEntity instructor = createInstructor("Instrutora Level");
        createCourse("Curso Ini", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso Adv", "Turismo", "AVANÇADO", CourseStatus.PUBLISHED, instructor);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, null, "INICIANTE", 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("Curso Ini", result.content().get(0).title());
    }

    @Test
    void shouldPaginateResults() {
        final AdminEntity instructor = createInstructor("Instrutora Paginacao");
        createCourse("Curso 1", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso 2", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);
        createCourse("Curso 3", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, instructor);

        final PageResult<CourseSummary> firstPage = courseRepositoryPort.findPublished(
                null, null, null, 0, 2);

        assertEquals(3, firstPage.totalElements());
        assertEquals(2, firstPage.content().size());
        assertEquals(2, firstPage.totalPages());

        final PageResult<CourseSummary> secondPage = courseRepositoryPort.findPublished(
                null, null, null, 1, 2);

        assertEquals(1, secondPage.content().size());
    }

    @Test
    void shouldIncludeInstructorName() {
        final AdminEntity instructor = createInstructor("Dra. Mariana");
        createCourse("Curso com Instrutor", "Turismo", "INICIANTE",
                CourseStatus.PUBLISHED, instructor);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, null, null, 0, 10);

        assertEquals("Dra. Mariana", result.content().get(0).instructorName());
    }

    @Test
    void shouldReturnEmptyWhenNoPublishedCourses() {
        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, null, null, 0, 10);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    @Test
    void shouldReturnDenormalizedCounters() {
        final AdminEntity instructor = createInstructor("Instrutora Contadores");
        final CourseEntity course = buildCourse("Curso Contadores", "Turismo", "INICIANTE",
                CourseStatus.PUBLISHED, instructor);
        course.setLessonsCount(5);
        course.setReviewsCount(10);
        course.setRatingAverage(4.5);
        courseJpaRepository.save(course);

        final PageResult<CourseSummary> result = courseRepositoryPort.findPublished(
                null, null, null, 0, 10);

        final CourseSummary summary = result.content().get(0);
        assertEquals(5, summary.lessonsCount());
        assertEquals(10, summary.reviewsCount());
        assertEquals(4.5, summary.ratingAverage());
    }

    @Test
    void shouldReturnPublishedCourseWithModulesMaterialsAndHiddenPaidUrls() {
        final CourseEntity course = persistCourseWithFullGraph(CourseStatus.PUBLISHED);

        final Optional<CourseDetails> found = courseRepositoryPort.findDetailsById(course.getId());

        assertTrue(found.isPresent());
        final CourseDetails details = found.get();
        assertEquals(course.getTitle(), details.title());
        assertEquals("https://vimeo.com/1226382615", details.teaserVideoUrl());
        assertEquals("https://cdn.example.com/avatar.png", details.instructor().avatarUrl());
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
    void shouldReturnEmptyDetailsWhenCourseIsNotPublished() {
        final CourseEntity course = persistCourseWithFullGraph(CourseStatus.DRAFT);

        final Optional<CourseDetails> found = courseRepositoryPort.findDetailsById(course.getId());

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnEmptyDetailsWhenCourseDoesNotExist() {
        final Optional<CourseDetails> found = courseRepositoryPort.findDetailsById(UUID.randomUUID());

        assertTrue(found.isEmpty());
    }

    @Test
    void shouldMapPublishedCourseWithoutOptionalRelatedData() {
        final CourseEntity course = buildCourse(
                "Curso sem dados opcionais", "Turismo", "INICIANTE", CourseStatus.PUBLISHED, null);
        courseJpaRepository.save(course);

        final CourseDetails details = courseRepositoryPort.findDetailsById(course.getId()).orElseThrow();

        assertNull(details.instructor());
        assertTrue(details.learningObjectives().isEmpty());
        assertTrue(details.materials().isEmpty());
        assertTrue(details.modules().isEmpty());
    }

    @Test
    void shouldUpdateCourseWithoutErasingCountersOrItsAssociations() {
        final AdminEntity instructorEntity = createInstructor("Instrutora Update");
        final CourseEntity persistedEntity = buildCourse(
                "Course before update", "Tourism", "Beginner", CourseStatus.PUBLISHED, instructorEntity);
        persistedEntity.setLessonsCount(7);
        persistedEntity.setMaterialsCount(3);
        persistedEntity.setStudentsCount(11);
        persistedEntity.setReviewsCount(5);
        persistedEntity.setRatingAverage(4.7);
        courseJpaRepository.save(persistedEntity);
        final User instructor = new User(
                instructorEntity.getId(), instructorEntity.getName(), instructorEntity.getEmail(),
                "hash", "ADMIN", null, instructorEntity.getCreatedAt());
        final Course updatedCourse = new Course(
                "Course after update", null, null, null, null, instructor,
                "Hospitality", "Advanced", 20, 30, 15.0, List.of("Objective"),
                false, true, instructor);
        updatedCourse.setId(persistedEntity.getId());

        courseRepositoryPort.save(updatedCourse);

        final CourseEntity reloaded = courseJpaRepository.findById(persistedEntity.getId()).orElseThrow();
        assertEquals("Course after update", reloaded.getTitle());
        assertEquals(instructorEntity.getId(), reloaded.getInstructor().getId());
        assertEquals(instructorEntity.getId(), reloaded.getCreatedBy().getId());
        assertEquals(7, reloaded.getLessonsCount());
        assertEquals(3, reloaded.getMaterialsCount());
        assertEquals(11, reloaded.getStudentsCount());
        assertEquals(5, reloaded.getReviewsCount());
        assertEquals(4.7, reloaded.getRatingAverage());
    }

    @Test
    void shouldTreatNullAndUnknownIdsAsNonexistent() {
        assertFalse(courseRepositoryPort.existsById(null));
        assertFalse(courseRepositoryPort.existsById(UUID.randomUUID()));

        final CourseEntity course = buildCourse(
                "Curso existente", "Turismo", "INICIANTE", CourseStatus.DRAFT, null);
        courseJpaRepository.save(course);

        assertTrue(courseRepositoryPort.existsById(course.getId()));
    }

    private AdminEntity createInstructor(String name) {
        final String uniqueEmail = name.toLowerCase().replace(" ", ".").replace(".", "")
                + UUID.randomUUID().toString().substring(0, 4) + "@escapa.com";
        final AdminEntity admin = new AdminEntity(
                UUID.randomUUID(), name, uniqueEmail, "hash", "ADMIN",
                LocalDateTime.now(), "Departamento");
        return userJpaRepository.save(admin);
    }

    private void createCourse(String title, String category, String level,
                              CourseStatus status, AdminEntity instructor) {
        final CourseEntity course = buildCourse(title, category, level, status, instructor);
        courseJpaRepository.save(course);
    }

    private CourseEntity buildCourse(String title, String category, String level,
                                    CourseStatus status, AdminEntity instructor) {
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
        return course;
    }

    private CourseEntity persistCourseWithFullGraph(CourseStatus status) {
        final AdminEntity instructor = createInstructor("Dra. Mariana Fonseca");
        instructor.setAvatarUrl("https://cdn.example.com/avatar.png");
        userJpaRepository.save(instructor);

        final CourseEntity course = buildCourse(
                "IA Aplicada ao Turismo", "Inteligencia Artificial", "Iniciante",
                status, instructor);
        course.setDescription("Descricao completa");
        course.setTeaserVideoUrl("https://vimeo.com/1226382615");
        course.setDurationTime(12);
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
