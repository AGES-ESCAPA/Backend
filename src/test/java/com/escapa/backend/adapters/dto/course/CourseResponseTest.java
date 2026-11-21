package com.escapa.backend.adapters.dto.course;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

class CourseResponseTest {

    @Test
    void shouldMapInstructorAndCreatorIds() {
        final User instructor = user("instructor@example.com");
        final User creator = user("creator@example.com");
        final Course course = new Course(
                "Curso", null, null, null, null, instructor, null, null,
                null, null, null, List.of(), true, false, creator);
        course.setId(UUID.randomUUID());

        final CourseResponse response = CourseResponse.from(course);

        assertEquals(instructor.getId(), response.instructorId());
        assertEquals(creator.getId(), response.createdBy());
    }

    private static User user(String email) {
        final User user = new User("Usuário", email, "hash", "ADMIN");
        user.setId(UUID.randomUUID());
        return user;
    }
}
