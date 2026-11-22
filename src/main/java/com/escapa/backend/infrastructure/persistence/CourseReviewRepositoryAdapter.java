package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.escapa.backend.application.port.CourseReviewRepositoryPort;

public class CourseReviewRepositoryAdapter implements CourseReviewRepositoryPort {

    private final JdbcClient jdbcClient;

    public CourseReviewRepositoryAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void saveRating(UUID userId, UUID courseId, int rating, LocalDateTime ratedAt) {
        jdbcClient.sql("""
                insert into course_reviews (id, course_id, user_id, rating, created_at, updated_at)
                values (:id, :courseId, :userId, :rating, :ratedAt, :ratedAt)
                on conflict (course_id, user_id)
                do update set rating = excluded.rating, updated_at = excluded.updated_at
                """)
                .param("id", UUID.randomUUID())
                .param("courseId", courseId)
                .param("userId", userId)
                .param("rating", rating)
                .param("ratedAt", ratedAt)
                .update();
    }

    @Override
    public Map<UUID, Integer> findRatingsByUser(UUID userId, Collection<UUID> courseIds) {
        final Map<UUID, Integer> ratings = new HashMap<>();
        if (courseIds.isEmpty()) {
            return ratings;
        }
        jdbcClient.sql("select course_id, rating from course_reviews "
                        + "where user_id = :userId and course_id in (:courseIds)")
                .param("userId", userId)
                .param("courseIds", courseIds)
                .query((rs, rowNum) -> Map.entry(rs.getObject("course_id", UUID.class), rs.getInt("rating")))
                .list()
                .forEach(entry -> ratings.put(entry.getKey(), entry.getValue()));
        return ratings;
    }
}
