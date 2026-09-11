package com.escapa.backend.course.shared.entity;

import com.escapa.backend.common.JpaIntegrationTest;
import com.escapa.backend.user.entity.AdminEntity;
import com.escapa.backend.user.entity.RegularUserEntity;
import com.escapa.backend.user.entity.UserEntity;
import com.escapa.backend.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Valida o trigger {@code trg_sync_review_counters} da migration V6:
 * {@code courses.reviews_count} e {@code courses.rating_average} acompanham
 * inserção, alteração de nota e remoção de avaliações.
 */
class ReviewCountersTriggerTest extends JpaIntegrationTest {

    @Autowired
    private TestEntityManager em;

    private CourseEntity course;
    private UserEntity alice;
    private UserEntity bob;

    @BeforeEach
    void setUp() {
        final AdminEntity instructor = em.persist(newAdmin());
        course = em.persist(newCourse("Curso Avaliado", instructor));
        alice = em.persist(newStudent("alice"));
        bob = em.persist(newStudent("bob"));
        em.flush();
        em.clear();
    }

    @Test
    void newCourseHasNoReviewsAndNoAverage() {
        final CourseEntity reloaded = reload();

        assertEquals(0, reloaded.getReviewsCount());
        assertNull(reloaded.getRatingAverage());
    }

    @Test
    void insertingReviewsUpdatesCountAndAverage() {
        review(alice, 5);
        em.flush();
        em.clear();
        assertEquals(1, reload().getReviewsCount());
        assertEquals(5.0, reload().getRatingAverage());

        review(bob, 4);
        em.flush();
        em.clear();
        assertEquals(2, reload().getReviewsCount());
        assertEquals(4.5, reload().getRatingAverage());
    }

    @Test
    void changingARatingRecalculatesTheAverage() {
        final CourseReviewEntity first = review(alice, 5);
        review(bob, 4);
        em.flush();
        em.clear();

        em.find(CourseReviewEntity.class, first.getId()).setRating(3);
        em.flush();
        em.clear();

        assertEquals(2, reload().getReviewsCount());
        assertEquals(3.5, reload().getRatingAverage());
    }

    @Test
    void deletingReviewsDecrementsAndClearsAverageWhenNoneRemain() {
        final CourseReviewEntity first = review(alice, 5);
        final CourseReviewEntity second = review(bob, 4);
        em.flush();
        em.clear();

        em.remove(em.find(CourseReviewEntity.class, first.getId()));
        em.flush();
        em.clear();
        assertEquals(1, reload().getReviewsCount());
        assertEquals(4.0, reload().getRatingAverage());

        em.remove(em.find(CourseReviewEntity.class, second.getId()));
        em.flush();
        em.clear();
        assertEquals(0, reload().getReviewsCount());
        assertNull(reload().getRatingAverage());
    }

    private CourseEntity reload() {
        return em.find(CourseEntity.class, course.getId());
    }

    private CourseReviewEntity review(UserEntity user, int rating) {
        final CourseReviewEntity review = new CourseReviewEntity();
        review.setCourse(em.find(CourseEntity.class, course.getId()));
        review.setUser(em.find(UserEntity.class, user.getId()));
        review.setRating(rating);
        review.setCreatedAt(LocalDateTime.now());
        return em.persist(review);
    }

    private static AdminEntity newAdmin() {
        return new AdminEntity(UUID.randomUUID(), "Instrutora",
                "rev." + UUID.randomUUID().toString().substring(0, 8) + "@escapa.com",
                "hash", UserRole.ADMIN, LocalDateTime.now(), "Turismo");
    }

    private static RegularUserEntity newStudent(String prefix) {
        return new RegularUserEntity(UUID.randomUUID(), prefix,
                prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@email.com",
                "hash", UserRole.STUDENT, LocalDateTime.now(), null, null);
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
