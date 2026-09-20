package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.port.EnrollmentRepositoryPort;
import com.escapa.backend.application.port.LessonRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.UserCourseId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentLessonAdaptersTest extends PostgresIntegrationTest {

    @Autowired
    private LessonRepositoryPort lessonRepositoryPort;

    @Autowired
    private EnrollmentRepositoryPort enrollmentRepositoryPort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private ModuleJpaRepository moduleJpaRepository;

    @Autowired
    private ContentJpaRepository contentJpaRepository;

    @Autowired
    private UserCourseJpaRepository userCourseJpaRepository;

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private CourseEntity givenCourse(boolean enforceDeadlineBlock) {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Curso de teste");
        course.setEnforceDeadlineBlock(enforceDeadlineBlock);
        return courseJpaRepository.save(course);
    }

    private ModuleEntity givenModule(CourseEntity course, String title, int order) {
        final ModuleEntity module = new ModuleEntity();
        module.setCourse(course);
        module.setTitle(title);
        module.setOrder(order);
        return moduleJpaRepository.save(module);
    }

    private ContentEntity givenContent(ModuleEntity module, String title, int order) {
        final ContentEntity content = new ContentEntity();
        content.setModule(module);
        content.setTitle(title);
        content.setDescription("Descricao");
        content.setType(ContentType.VIDEO);
        content.setUrl("https://cdn.example.com/video.mp4");
        content.setIsFree(false);
        content.setOrder(order);
        content.setCreatedAt(LocalDateTime.now());
        return contentJpaRepository.save(content);
    }

    @Test
    void shouldFindLessonWithModuleAndCourseRule() {
        final CourseEntity course = givenCourse(true);
        final ModuleEntity module = givenModule(course, "Fundamentos", 1);
        final ContentEntity content = givenContent(module, "Aula 1", 1);

        final Optional<LessonDetails> found =
                lessonRepositoryPort.findByCourseIdAndId(course.getId(), content.getId());

        assertTrue(found.isPresent());
        assertEquals("Aula 1", found.get().content().getTitle());
        assertEquals(module.getId(), found.get().content().getModuleId());
        assertEquals("Fundamentos", found.get().moduleTitle());
        assertEquals(1, found.get().moduleOrder());
        assertEquals(course.getId(), found.get().courseId());
        assertTrue(found.get().courseEnforcesDeadlineBlock());
    }

    @Test
    void shouldNotFindLessonThroughAnotherCourse() {
        final CourseEntity course = givenCourse(false);
        final CourseEntity otherCourse = givenCourse(false);
        final ContentEntity content = givenContent(givenModule(course, "Modulo", 1), "Aula 1", 1);

        assertTrue(lessonRepositoryPort.findByCourseIdAndId(otherCourse.getId(), content.getId()).isEmpty());
    }

    @Test
    void shouldNotFindUnknownLesson() {
        final CourseEntity course = givenCourse(false);

        assertTrue(lessonRepositoryPort.findByCourseIdAndId(course.getId(), UUID.randomUUID()).isEmpty());
    }

    @Test
    void shouldFindEnrollmentPeriodByUserAndCourse() {
        final CourseEntity course = givenCourse(false);
        final UUID userId = givenUserId();
        final LocalDate start = LocalDate.of(2026, 1, 10);
        final LocalDate expiration = LocalDate.of(2027, 1, 10);
        givenEnrollment(userId, course, start, expiration);

        final Optional<Enrollment> found = enrollmentRepositoryPort.findByUserIdAndCourseId(userId, course.getId());

        assertTrue(found.isPresent());
        assertEquals(start, found.get().startDate());
        assertEquals(expiration, found.get().expirationDate());
    }

    @Test
    void shouldNotFindEnrollmentOfAnotherUser() {
        final CourseEntity course = givenCourse(false);
        givenEnrollment(givenUserId(), course, LocalDate.of(2026, 1, 10), null);

        assertTrue(enrollmentRepositoryPort.findByUserIdAndCourseId(givenUserId(), course.getId()).isEmpty());
    }

    private UUID givenUserId() {
        final String email = "aluno-" + UUID.randomUUID() + "@email.com";
        return userRepositoryPort.save(new User("Aluno Teste", email, "hash", "STUDENT")).getId();
    }

    private void givenEnrollment(UUID userId, CourseEntity course, LocalDate start, LocalDate expiration) {
        final UserCourseEntity enrollment = new UserCourseEntity();
        enrollment.setId(new UserCourseId(userId, course.getId()));
        enrollment.setUser(userJpaRepository.getReferenceById(userId));
        enrollment.setCourse(course);
        enrollment.setDtInicio(start);
        enrollment.setDtExpiracao(expiration);
        userCourseJpaRepository.save(enrollment);
    }
}
