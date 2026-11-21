package com.escapa.backend.application.usecase;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.escapa.backend.application.port.CourseReviewRepositoryPort;

public final class InMemoryCourseReviewRepositoryPort implements CourseReviewRepositoryPort {
    private record Key(UUID userId, UUID courseId) {
    }

    private final Map<Key, Integer> ratings = new HashMap<>();

    @Override
    public void saveRating(UUID userId, UUID courseId, int rating, LocalDateTime ratedAt) {
        ratings.put(new Key(userId, courseId), rating);
    }

    @Override
    public Map<UUID, Integer> findRatingsByUser(UUID userId, Collection<UUID> courseIds) {
        final Map<UUID, Integer> result = new HashMap<>();
        for (final UUID courseId : courseIds) {
            final Integer rating = ratings.get(new Key(userId, courseId));
            if (rating != null) {
                result.put(courseId, rating);
            }
        }
        return result;
    }

    public Integer ratingOf(UUID userId, UUID courseId) {
        return ratings.get(new Key(userId, courseId));
    }
}
