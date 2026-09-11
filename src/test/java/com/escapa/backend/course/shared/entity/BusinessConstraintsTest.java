package com.escapa.backend.course.shared.entity;

import com.escapa.backend.common.JpaIntegrationTest;
import com.escapa.backend.user.entity.AdminEntity;
import com.escapa.backend.user.entity.UserRole;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prova que as redes de segurança da migration V6 estão aplicadas. Não testa cada CHECK:
 * uma amostra por família (enum, faixa, ordem, data) basta para detectar migration ausente.
 * Cada teste viola uma constraint, então cada um vive na própria transação.
 */
class BusinessConstraintsTest extends JpaIntegrationTest {

    @Autowired
    private TestEntityManager em;

    private AdminEntity instructor;

    @BeforeEach
    void setUp() {
        instructor = em.persist(new AdminEntity(UUID.randomUUID(), "Instrutora",
                "ck." + UUID.randomUUID().toString().substring(0, 8) + "@escapa.com",
                "hash", UserRole.ADMIN, LocalDateTime.now(), "Turismo"));
        em.flush();
    }

    @Test
    void roleOutsideTheVocabularyIsRejected() {
        final PersistenceException ex = assertThrows(PersistenceException.class, () -> em.getEntityManager()
                .createNativeQuery("""
                        INSERT INTO users (id, name, email, password_hash, role, status, created_at)
                        VALUES (:id, 'X', :email, 'h', 'TEACHER', 'ACTIVE', now())
                        """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("email", "teacher." + UUID.randomUUID() + "@email.com")
                .executeUpdate());

        assertViolates(ex, "ck_users_role");
    }

    @Test
    void negativePriceIsRejected() {
        final CourseEntity course = newCourse();
        course.setPrice(-1.0);

        final PersistenceException ex = assertThrows(PersistenceException.class, () -> {
            em.persist(course);
            em.flush();
        });

        assertViolates(ex, "ck_courses_price");
    }

    @Test
    void moduleOrderMustBePositive() {
        final CourseEntity course = em.persist(newCourse());
        final ModuleEntity module = new ModuleEntity();
        module.setCourse(course);
        module.setTitle("Zero");
        module.setOrder(0);

        final PersistenceException ex = assertThrows(PersistenceException.class, () -> {
            em.persist(module);
            em.flush();
        });

        assertViolates(ex, "ck_modules_order");
    }

    @Test
    void progressAbove100IsRejected() {
        final CourseEntity course = em.persist(newCourse());
        em.flush();

        final PersistenceException ex = assertThrows(PersistenceException.class, () -> em.getEntityManager()
                .createNativeQuery("""
                        INSERT INTO user_courses (user_id, course_id, progress, certificate_issued)
                        VALUES (:user, :course, 101, FALSE)
                        """)
                .setParameter("user", instructor.getId())
                .setParameter("course", course.getId())
                .executeUpdate());

        assertViolates(ex, "ck_user_courses_progress");
    }

    @Test
    void enrollmentCannotExpireBeforeItStarts() {
        final CourseEntity course = em.persist(newCourse());
        em.flush();

        final PersistenceException ex = assertThrows(PersistenceException.class, () -> em.getEntityManager()
                .createNativeQuery("""
                        INSERT INTO user_courses (user_id, course_id, dt_inicio, dt_expiracao, certificate_issued)
                        VALUES (:user, :course, DATE '2026-03-01', DATE '2026-02-01', FALSE)
                        """)
                .setParameter("user", instructor.getId())
                .setParameter("course", course.getId())
                .executeUpdate());

        assertViolates(ex, "ck_user_courses_dates");
    }

    private static void assertViolates(Throwable ex, String constraint) {
        Throwable cause = ex;
        final StringBuilder chain = new StringBuilder();
        while (cause != null) {
            chain.append(cause.getMessage()).append(" | ");
            cause = cause.getCause();
        }
        assertTrue(chain.toString().contains(constraint),
                "esperava violação de " + constraint + " mas a cadeia de erro foi: " + chain);
    }

    private CourseEntity newCourse() {
        final CourseEntity c = new CourseEntity();
        c.setTitle("Curso " + UUID.randomUUID());
        c.setStatus(CourseStatus.DRAFT);
        c.setInstructor(instructor);
        c.setCreatedBy(instructor);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }
}
