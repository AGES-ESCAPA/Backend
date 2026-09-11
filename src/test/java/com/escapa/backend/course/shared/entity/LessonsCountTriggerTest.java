package com.escapa.backend.course.shared.entity;

import com.escapa.backend.common.JpaIntegrationTest;
import com.escapa.backend.user.entity.AdminEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Valida o trigger {@code trg_sync_lessons_count} da migration V5: {@code courses.lessons_count}
 * acompanha inserção, remoção e troca de módulo de uma aula.
 *
 * <p>O trigger roda no banco, então cada passo faz {@code flush} (dispara o SQL) e {@code clear}
 * (esvazia o cache do Hibernate) antes de reler o curso. Sem o {@code clear}, o teste leria o
 * valor antigo que ficou em memória.
 */
class LessonsCountTriggerTest extends JpaIntegrationTest {

    @Autowired
    private TestEntityManager em;

    private CourseEntity courseA;
    private ModuleEntity moduleA;

    @BeforeEach
    void setUp() {
        final AdminEntity instructor = em.persist(new AdminEntity(UUID.randomUUID(), "Instrutora",
                "trigger." + UUID.randomUUID().toString().substring(0, 8) + "@escapa.com",
                "hash", "ADMIN", LocalDateTime.now(), "Turismo"));
        courseA = newCourse("Curso A", instructor);
        moduleA = newModule(courseA, 1);
        em.flush();
        em.clear();
    }

    @Test
    void newCourseStartsWithZeroLessons() {
        assertEquals(0, reload(courseA).getLessonsCount());
    }

    @Test
    void insertingLessonsIncrementsTheCounter() {
        newContent(moduleA, 1);
        newContent(moduleA, 2);
        em.flush();
        em.clear();

        assertEquals(2, reload(courseA).getLessonsCount());
    }

    @Test
    void deletingALessonDecrementsTheCounter() {
        final ContentEntity first = newContent(moduleA, 1);
        newContent(moduleA, 2);
        em.flush();
        em.clear();

        em.remove(em.find(ContentEntity.class, first.getId()));
        em.flush();
        em.clear();

        assertEquals(1, reload(courseA).getLessonsCount());
    }

    @Test
    void movingALessonToAnotherCourseUpdatesBothCounters() {
        final AdminEntity other = em.persist(new AdminEntity(UUID.randomUUID(), "Outro",
                "trigger2." + UUID.randomUUID().toString().substring(0, 8) + "@escapa.com",
                "hash", "ADMIN", LocalDateTime.now(), "Turismo"));
        final CourseEntity courseB = newCourse("Curso B", other);
        final ModuleEntity moduleB = newModule(courseB, 1);
        final ContentEntity lesson = newContent(moduleA, 1);
        em.flush();
        em.clear();
        assertEquals(1, reload(courseA).getLessonsCount());
        assertEquals(0, reload(courseB).getLessonsCount());

        final ContentEntity managed = em.find(ContentEntity.class, lesson.getId());
        managed.setModule(em.find(ModuleEntity.class, moduleB.getId()));
        em.flush();
        em.clear();

        assertEquals(0, reload(courseA).getLessonsCount());
        assertEquals(1, reload(courseB).getLessonsCount());
    }

    private CourseEntity reload(CourseEntity course) {
        return em.find(CourseEntity.class, course.getId());
    }

    private CourseEntity newCourse(String title, AdminEntity instructor) {
        final CourseEntity course = new CourseEntity();
        course.setTitle(title);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setInstructor(instructor);
        course.setCreatedBy(instructor);
        course.setCreatedAt(LocalDateTime.now());
        return em.persist(course);
    }

    private ModuleEntity newModule(CourseEntity course, int order) {
        final ModuleEntity module = new ModuleEntity();
        module.setCourse(course);
        module.setTitle("Módulo " + order);
        module.setOrder(order);
        return em.persist(module);
    }

    private ContentEntity newContent(ModuleEntity module, int order) {
        final ContentEntity content = new ContentEntity();
        content.setModule(module);
        content.setTitle("Aula " + order);
        content.setType(ContentType.VIDEO);
        content.setOrder(order);
        content.setIsFree(false);
        content.setCreatedAt(LocalDateTime.now());
        return em.persist(content);
    }
}
