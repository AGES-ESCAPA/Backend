package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.application.model.Enrollment;
import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.usecase.GetStudentLessonUseCase;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.entity.Content;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mapeamento HTTP da US-11: o caso de uso real roda sobre portas em memoria. */
class StudentLessonControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final String URL = "/api/v1/student/courses/{courseId}/lessons/{lessonId}";

    private final UUID userId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID moduleId = UUID.randomUUID();
    private final UUID paidLessonId = UUID.randomUUID();
    private final UUID freeLessonId = UUID.randomUUID();

    private MockMvc mockMvc;
    private Optional<Enrollment> enrollment = Optional.empty();

    @BeforeEach
    void setUp() {
        final LessonDetails paid = lesson(paidLessonId, false);
        final LessonDetails free = lesson(freeLessonId, true);
        final GetStudentLessonUseCase useCase = new GetStudentLessonUseCase(
                (course, lessonId) -> Optional.of(paid).filter(l -> l.content().getId().equals(lessonId))
                        .or(() -> Optional.of(free).filter(l -> l.content().getId().equals(lessonId)))
                        .filter(l -> l.courseId().equals(course)),
                new com.escapa.backend.application.port.EnrollmentRepositoryPort() {
                    @Override
                    public Optional<Enrollment> findByUserIdAndCourseId(UUID user, UUID course) {
                        return user.equals(userId) ? enrollment : Optional.empty();
                    }

                    @Override
                    public com.escapa.backend.application.dto.PageResult<com.escapa.backend.application.model.StudentCourseCard> findStudentEnrollments(UUID userId, String title, com.escapa.backend.domain.course.EnrollmentStatus status, int page, int size) {
                        return null;
                    }
                },
                Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(new StudentLessonController(useCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private LessonDetails lesson(UUID id, boolean isFree) {
        final Content content = new Content(
                id, moduleId, "Aula 2", "Descricao da aula", ContentType.VIDEO,
                "https://cdn.example.com/aula-2.mp4", 12, isFree, 2, null, null);
        return new LessonDetails(content, courseId, "Fundamentos", 1, true);
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
}
