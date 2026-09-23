package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.StudentCourseCurriculum;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.LessonAccessDeniedException;
import com.escapa.backend.domain.content.LessonAccessDeniedException.Reason;
import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetStudentCourseCurriculumUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private Course course;
    private Optional<Enrollment> enrollment = Optional.empty();
    private List<Module> modules = List.of();

    private GetStudentCourseCurriculumUseCase buildUseCase() {
    return new GetStudentCourseCurriculumUseCase(
            stubCourseRepositoryPort(),
            stubModuleRepositoryPort(),
            (user, courseArg) -> List.of(),
            moduleId -> List.of(),
            (user, courseArg) -> user.equals(userId) ? enrollment : Optional.empty(),
            clock
    );
}

private CourseRepositoryPort stubCourseRepositoryPort() {
    return new CourseRepositoryPort() {
        @Override
        public Course save(Course courseArg) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Course> findById(UUID id) {
            return id.equals(courseId) ? Optional.ofNullable(course) : Optional.empty();
        }

        @Override
        public boolean existsById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Course> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Course> searchByTitle(String title, UUID excludedCourseId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PageResult<CourseSummary> findPublished(
                String title, String category, String level, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CourseDetails> findDetailsById(UUID id) {
            throw new UnsupportedOperationException();
        }
    };
}

    private ModuleRepositoryPort stubModuleRepositoryPort() {
        return new ModuleRepositoryPort() {
            @Override
            public boolean existsById(UUID id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Module save(Module module) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<Module> findById(UUID id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<Module> findByCourseId(UUID courseIdArg) {
                return modules;
            }

            @Override
            public int nextOrder(UUID courseIdArg) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void deleteById(UUID id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void reorder(UUID courseIdArg, List<UUID> orderedIds) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private Course givenCourse(boolean enforceDeadlineBlock) {
        final Course c = new Course();
        c.setId(courseId);
        c.setTitle("Curso Teste");
        c.setRequireSequentialProgress(true);
        c.setEnforceDeadlineBlock(enforceDeadlineBlock);
        return c;
    }

    private Module givenModuleWithLesson() {
        final Module module = new Module(UUID.randomUUID(), courseId, "Modulo 1", 1);
        final Content content = new Content(
                UUID.randomUUID(), module.getId(), "Aula 1", "desc", ContentType.VIDEO,
                "https://cdn.example.com/video.mp4", 10, false, 1, null, null);
        module.getContents().add(content);
        return module;
    }

    @Test
    void shouldRejectWhenCourseDoesNotExist() {
        course = null;
        assertThrows(CourseNotFoundException.class, () -> buildUseCase().execute(userId, courseId));
    }

    @Test
    void shouldDenyWhenNotEnrolled() {
        course = givenCourse(false);
        final LessonAccessDeniedException ex = assertThrows(
                LessonAccessDeniedException.class, () -> buildUseCase().execute(userId, courseId));
        assertEquals(Reason.NOT_ENROLLED, ex.getReason());
    }

    @Test
    void shouldDenyWhenEnrollmentHasNotStarted() {
        course = givenCourse(false);
        enrollment = Optional.of(new Enrollment(TODAY.plusDays(1), TODAY.plusDays(60)));

        final LessonAccessDeniedException ex = assertThrows(
                LessonAccessDeniedException.class, () -> buildUseCase().execute(userId, courseId));
        assertEquals(Reason.ENROLLMENT_NOT_STARTED, ex.getReason());
    }

    @Test
    void shouldDenyWhenAccessExpiredAndCourseBlocksByDeadline() {
        course = givenCourse(true);
        enrollment = Optional.of(new Enrollment(TODAY.minusDays(90), TODAY.minusDays(1)));

        final LessonAccessDeniedException ex = assertThrows(
                LessonAccessDeniedException.class, () -> buildUseCase().execute(userId, courseId));
        assertEquals(Reason.ACCESS_EXPIRED, ex.getReason());
    }

    @Test
    void shouldReturnCurriculumForEnrolledStudent() {
        course = givenCourse(false);
        enrollment = Optional.of(new Enrollment(TODAY.minusDays(10), TODAY.plusDays(90)));
        modules = List.of(givenModuleWithLesson());

        final StudentCourseCurriculum result = buildUseCase().execute(userId, courseId);

        assertEquals(courseId, result.courseId());
        assertEquals(1, result.totalLessons());
        assertEquals(1, result.modules().size());
        assertEquals("Modulo 1", result.modules().get(0).title());
    }
}