package com.escapa.backend.course.shared.entity;

import com.escapa.backend.common.JpaIntegrationTest;
import com.escapa.backend.user.entity.AdminEntity;
import com.escapa.backend.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Valida o trigger {@code trg_sync_materials_count} da migration V6:
 * {@code courses.materials_count} acompanha inserção, remoção e troca de curso de um material.
 */
class MaterialsCountTriggerTest extends JpaIntegrationTest {

    @Autowired
    private TestEntityManager em;

    private CourseEntity courseA;
    private CourseEntity courseB;

    @BeforeEach
    void setUp() {
        final AdminEntity instructor = em.persist(new AdminEntity(UUID.randomUUID(), "Instrutora",
                "mat." + UUID.randomUUID().toString().substring(0, 8) + "@escapa.com",
                "hash", UserRole.ADMIN, LocalDateTime.now(), "Turismo"));
        courseA = em.persist(newCourse("Curso A", instructor));
        courseB = em.persist(newCourse("Curso B", instructor));
        em.flush();
        em.clear();
    }

    @Test
    void newCourseStartsWithZeroMaterials() {
        assertEquals(0, reload(courseA).getMaterialsCount());
    }

    @Test
    void insertingAndDeletingMaterialsKeepsTheCounter() {
        final CourseMaterialEntity first = material(courseA, "Checklist");
        material(courseA, "Planilha");
        em.flush();
        em.clear();
        assertEquals(2, reload(courseA).getMaterialsCount());

        em.remove(em.find(CourseMaterialEntity.class, first.getId()));
        em.flush();
        em.clear();
        assertEquals(1, reload(courseA).getMaterialsCount());
    }

    @Test
    void movingAMaterialToAnotherCourseUpdatesBothCounters() {
        final CourseMaterialEntity m = material(courseA, "Roteiro");
        em.flush();
        em.clear();
        assertEquals(1, reload(courseA).getMaterialsCount());
        assertEquals(0, reload(courseB).getMaterialsCount());

        em.find(CourseMaterialEntity.class, m.getId()).setCourse(em.find(CourseEntity.class, courseB.getId()));
        em.flush();
        em.clear();

        assertEquals(0, reload(courseA).getMaterialsCount());
        assertEquals(1, reload(courseB).getMaterialsCount());
    }

    private CourseEntity reload(CourseEntity course) {
        return em.find(CourseEntity.class, course.getId());
    }

    private CourseMaterialEntity material(CourseEntity course, String title) {
        final CourseMaterialEntity m = new CourseMaterialEntity();
        m.setCourse(em.find(CourseEntity.class, course.getId()));
        m.setTitle(title);
        m.setFileUrl("https://cdn.escapa.com/materials/" + title.toLowerCase() + ".pdf");
        m.setCreatedAt(LocalDateTime.now());
        return em.persist(m);
    }

    private static CourseEntity newCourse(String title, AdminEntity instructor) {
        final CourseEntity c = new CourseEntity();
        c.setTitle(title);
        c.setStatus(CourseStatus.PUBLISHED);
        c.setInstructor(instructor);
        c.setCreatedBy(instructor);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }
}
