package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CourseReviewRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseReviewRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CourseReviewRepositoryPort courseReviewRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID givenCourse() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Hospitalidade " + UUID.randomUUID());
        return courseJpaRepository.save(course).getId();
    }

    private UUID givenUser() {
        final String email = "aluno-" + UUID.randomUUID() + "@email.com";
        return userRepositoryPort.save(new User("Aluno", email, "hash", "STUDENT")).getId();
    }

    @Test
    void shouldReplaceTheRatingWhenTheStudentRatesAgain() {
        final UUID userId = givenUser();
        final UUID courseId = givenCourse();

        courseReviewRepositoryPort.saveRating(userId, courseId, 2, LocalDateTime.now());
        courseReviewRepositoryPort.saveRating(userId, courseId, 5, LocalDateTime.now());

        assertEquals(Map.of(courseId, 5), courseReviewRepositoryPort.findRatingsByUser(userId, List.of(courseId)));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from course_reviews where user_id = ? and course_id = ?",
                Integer.class, userId, courseId));
    }

    @Test
    void shouldReturnOnlyTheRatingsOfTheStudentForTheRequestedCourses() {
        final UUID userId = givenUser();
        final UUID otherUserId = givenUser();
        final UUID ratedCourse = givenCourse();
        final UUID notRequestedCourse = givenCourse();
        final UUID ratedByOtherCourse = givenCourse();
        courseReviewRepositoryPort.saveRating(userId, ratedCourse, 4, LocalDateTime.now());
        courseReviewRepositoryPort.saveRating(userId, notRequestedCourse, 3, LocalDateTime.now());
        courseReviewRepositoryPort.saveRating(otherUserId, ratedByOtherCourse, 1, LocalDateTime.now());

        final Map<UUID, Integer> ratings =
                courseReviewRepositoryPort.findRatingsByUser(userId, List.of(ratedCourse, ratedByOtherCourse));

        assertEquals(Map.of(ratedCourse, 4), ratings);
    }

    @Test
    void shouldReturnNoRatingsForAnEmptyCourseList() {
        assertTrue(courseReviewRepositoryPort.findRatingsByUser(givenUser(), List.of()).isEmpty());
    }
}
