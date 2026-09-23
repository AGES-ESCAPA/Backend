package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.dto.CourseSummary;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.application.usecase.GetStudentCourseCurriculumUseCase;
import com.escapa.backend.application.usecase.GetStudentLessonUseCase;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mapeamento HTTP da US-11 (aula) e da US-13 (grade de aulas). */
class StudentLessonControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final String URL = "/api/v1/student/courses/{courseId}/lessons/{lessonId}";
    private static final String CURRICULUM_URL = "/api/v1/student/courses/{courseId}/curriculum";

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID moduleId = UUID.randomUUID();
    private final UUID paidLessonId = UUID.randomUUID();
    private final UUID freeLessonId = UUID.randomUUID();

    private MockMvc mockMvc;
    private Optional<Enrollment> enrollment = Optional.empty();

    private Optional<Course> curriculumCourse = Optional.empty();
    private List<Module> curriculumModules = List.of();
    private Optional<Enrollment> curriculumEnrollment = Optional.empty();

    @BeforeEach
    void setUp() {
        final LessonDetails paid = lesson(paidLessonId, false);
        final LessonDetails free = lesson(freeLessonId, true);
        final GetStudentLessonUseCase useCase = new GetStudentLessonUseCase(
                (course, lessonId) -> Optional.of(paid).filter(l -> l.content().getId().equals(lessonId))
                        .or(() -> Optional.of(free).filter(l -> l.content().getId().equals(lessonId)))
                        .filter(l -> l.courseId().equals(course)),
                (user, course) -> user.equals(userId) ? enrollment : Optional.empty(),
                Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        );

        final CourseRepositoryPort courseRepositoryPort = new CourseRepositoryPort() {
            @Override
            public Course save(Course course) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<Course> findById(UUID id) {
                return curriculumCourse.filter(c -> c.getId().equals(id));
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

        final ModuleRepositoryPort moduleRepositoryPort = new ModuleRepositoryPort() {
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
                return curriculumModules;
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

        final GetStudentCourseCurriculumUseCase curriculumUseCase = new GetStudentCourseCurriculumUseCase(
                courseRepositoryPort,
                moduleRepositoryPort,
                (user, course) -> List.of(),
                moduleId -> List.of(),
                (user, course) -> user.equals(userId) ? curriculumEnrollment : Optional.empty(),
                Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        );

        mockMvc = MockMvcBuilders.standaloneSetup(new StudentLessonController(useCase, curriculumUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private LessonDetails lesson(UUID id, boolean isFree) {
        final Content content = new Content(
                id, moduleId, "Aula 2", "Descricao da aula", ContentType.VIDEO,
                "https://cdn.example.com/aula-2.mp4", 12, isFree, 2, null, null);
        return new LessonDetails(content, courseId, "Fundamentos", 1, true);
    }

    private Course givenCourse() {
        final Course course = new Course();
        course.setId(courseId);
        course.setTitle("Curso Teste");
        course.setRequireSequentialProgress(true);
        course.setEnforceDeadlineBlock(false);
        return course;
    }

    private Module givenModuleWithLesson() {
        final Module module = new Module(UUID.randomUUID(), courseId, "Modulo 1", 1);
        final Content content = new Content(
                UUID.randomUUID(), module.getId(), "Aula 1", "Descricao", ContentType.VIDEO,
                "https://cdn.example.com/aula-1.mp4", 10, false, 1, null, null);
        module.getContents().add(content);
        return module;
    }

    @Test
    void shouldReturn200WithLessonAndModuleHeaderData() throws Exception {
        enrollment = Optional.of(new Enrollment(TODAY.minusDays(5), TODAY.plusDays(60)));

        mockMvc.perform(get(URL, courseId, paidLessonId).header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(paidLessonId.toString()))
                .andExpect(jsonPath("$.data.title").value("Aula 2"))
                .andExpect(jsonPath("$.data.description").value("Descricao da aula"))
                .andExpect(jsonPath("$.data.type").value("VIDEO"))
                .andExpect(jsonPath("$.data.url").value("https://cdn.example.com/aula-2.mp4"))
                .andExpect(jsonPath("$.data.order").value(2))
                .andExpect(jsonPath("$.data.module.id").value(moduleId.toString()))
                .andExpect(jsonPath("$.data.module.title").value("Fundamentos"))
                .andExpect(jsonPath("$.data.module.order").value(1));
    }

    @Test
    void shouldReturn200ForFreeLessonWithoutEnrollment() throws Exception {
        mockMvc.perform(get(URL, courseId, freeLessonId).header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFree").value(true));
    }

    @Test
    void shouldReturn401WhenHeaderIsMissing() throws Exception {
        mockMvc.perform(get(URL, courseId, freeLessonId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenHeaderIsBlank() throws Exception {
        mockMvc.perform(get(URL, courseId, freeLessonId).header("X-User-Id", " "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenHeaderIsNotAUuid() throws Exception {
        mockMvc.perform(get(URL, courseId, freeLessonId).header("X-User-Id", "not-a-uuid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404WhenLessonDoesNotExist() throws Exception {
        mockMvc.perform(get(URL, courseId, UUID.randomUUID()).header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenLessonBelongsToAnotherCourse() throws Exception {
        mockMvc.perform(get(URL, UUID.randomUUID(), freeLessonId).header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenPaidLessonAndNotEnrolled() throws Exception {
        mockMvc.perform(get(URL, courseId, paidLessonId).header("X-User-Id", userId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenEnrollmentHasNotStarted() throws Exception {
        enrollment = Optional.of(new Enrollment(TODAY.plusDays(1), TODAY.plusDays(60)));

        mockMvc.perform(get(URL, courseId, paidLessonId).header("X-User-Id", userId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenDeadlineExpiredOnCourseWithDeadlineBlock() throws Exception {
        enrollment = Optional.of(new Enrollment(TODAY.minusDays(90), TODAY.minusDays(1)));

        mockMvc.perform(get(URL, courseId, paidLessonId).header("X-User-Id", userId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400WhenLessonIdIsNotAUuid() throws Exception {
        mockMvc.perform(get(URL, courseId, "abc").header("X-User-Id", userId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200WithCurriculumForEnrolledStudent() throws Exception {
        curriculumCourse = Optional.of(givenCourse());
        curriculumEnrollment = Optional.of(new Enrollment(TODAY.minusDays(10), TODAY.plusDays(90)));
        curriculumModules = List.of(givenModuleWithLesson());

        mockMvc.perform(get(CURRICULUM_URL, courseId).header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseId").value(courseId.toString()))
                .andExpect(jsonPath("$.data.totalLessons").value(1))
                .andExpect(jsonPath("$.data.modules[0].title").value("Modulo 1"))
                .andExpect(jsonPath("$.data.modules[0].lessons[0].title").value("Aula 1"))
                .andExpect(jsonPath("$.data.modules[0].lessons[0].status").value("AVAILABLE"));
    }

    @Test
    void shouldReturn401WhenHeaderIsMissingForCurriculum() throws Exception {
        mockMvc.perform(get(CURRICULUM_URL, courseId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenNotEnrolledInCurriculum() throws Exception {
        curriculumCourse = Optional.of(givenCourse());

        mockMvc.perform(get(CURRICULUM_URL, courseId).header("X-User-Id", userId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenCourseDoesNotExistForCurriculum() throws Exception {
        mockMvc.perform(get(CURRICULUM_URL, courseId).header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }
}