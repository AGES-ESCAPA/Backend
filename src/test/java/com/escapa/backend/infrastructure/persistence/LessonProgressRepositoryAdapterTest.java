package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.LessonProgressRepositoryPort;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserContentProgressEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserContentProgressId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonProgressRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private LessonProgressRepositoryPort lessonProgressRepositoryPort;

    @Autowired
    private ContentRepositoryPort contentRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private ModuleJpaRepository moduleJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserContentProgressJpaRepository progressJpaRepository;

    private UUID givenUser() {
        final UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setName("Aluno de teste");
        user.setEmail("aluno-" + UUID.randomUUID() + "@teste.com");
        user.setPasswordHash("hash");
        user.setRole("STUDENT");
        user.setCreatedAt(LocalDateTime.now());
        return userJpaRepository.save(user).getId();
    }

    private UUID givenModule(String courseTitle) {
        final CourseEntity course = new CourseEntity();
        course.setTitle(courseTitle);
        final CourseEntity savedCourse = courseJpaRepository.save(course);

        final ModuleEntity module = new ModuleEntity();
        module.setCourse(savedCourse);
        module.setTitle("Modulo de teste");
        module.setOrder(1);
        return moduleJpaRepository.save(module).getId();
    }

    private UUID givenContent(UUID moduleId, String title) {
        final Content content = new Content();
        content.setModuleId(moduleId);
        content.setTitle(title);
        content.setType(ContentType.TEXT);
        content.setDescription("Texto");
        content.setIsFree(false);
        content.setOrder(contentRepositoryPort.nextOrder(moduleId));
        return contentRepositoryPort.save(content).getId();
    }

    private void givenProgress(UUID userId, UUID contentId) {
        final UserContentProgressEntity progress = new UserContentProgressEntity();
        progress.setId(new UserContentProgressId(userId, contentId));
        progress.setCompletedAt(LocalDateTime.now());
        progressJpaRepository.save(progress);
    }

    @Test
    void shouldReturnOnlyCompletedLessonsFromTheGivenCourse() {
        final UUID userId = givenUser();

        final UUID moduleInCourseA = givenModule("Curso A");
        final UUID lessonInCourseA = givenContent(moduleInCourseA, "Aula do curso A");

        final UUID moduleInCourseB = givenModule("Curso B");
        final UUID lessonInCourseB = givenContent(moduleInCourseB, "Aula do curso B");

        // Aluno concluiu aulas nos dois cursos.
        givenProgress(userId, lessonInCourseA);
        givenProgress(userId, lessonInCourseB);

        final UUID courseAId = moduleJpaRepository.findById(moduleInCourseA).orElseThrow()
                .getCourse().getId();

        final List<UUID> completed = lessonProgressRepositoryPort.findCompletedLessonIds(userId, courseAId);

        assertEquals(1, completed.size());
        assertTrue(completed.contains(lessonInCourseA));
        assertTrue(!completed.contains(lessonInCourseB));
    }
}
