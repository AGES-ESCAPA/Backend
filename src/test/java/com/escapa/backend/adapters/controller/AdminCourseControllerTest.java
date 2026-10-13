package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.exception.GlobalExceptionHandler;
import com.escapa.backend.adapters.security.AdminRequestGuard;
import com.escapa.backend.application.usecase.AddCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.ArchiveCourseUseCase;
import com.escapa.backend.application.usecase.AuthorizeAdminUseCase;
import com.escapa.backend.application.usecase.CreateCourseUseCase;
import com.escapa.backend.application.usecase.GetAdminCourseUseCase;
import com.escapa.backend.application.usecase.GetCourseChangeLogUseCase;
import com.escapa.backend.application.usecase.GetCourseRulesUseCase;
import com.escapa.backend.application.usecase.InMemoryCourseChangeLogRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryCourseNotificationPort;
import com.escapa.backend.application.usecase.InMemoryCoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryCourseRepositoryPort;
import com.escapa.backend.application.usecase.InMemoryUserRepositoryPort;
import com.escapa.backend.application.usecase.ListAdminCoursesUseCase;
import com.escapa.backend.application.usecase.ListCourseCategoriesUseCase;
import com.escapa.backend.application.usecase.PublishCourseUseCase;
import com.escapa.backend.application.usecase.RemoveCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.SearchCoursesForPrerequisiteUseCase;
import com.escapa.backend.application.usecase.UpdateCourseUseCase;
import com.escapa.backend.application.usecase.UpdateProgressRulesUseCase;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mapeamento HTTP, controle de acesso ADMIN e formato das respostas do CRUD administrativo de cursos. */
class AdminCourseControllerTest {

    private static final String USER_HEADER = "X-User-Id";
    private static final String COURSES_URL = "/api/v1/admin/courses";

    private final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
    private final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
    private final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
    private final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
            new InMemoryCoursePrerequisiteRepositoryPort();

    private String adminId;
    private String studentId;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN")).getId().toString();
        studentId = userRepository.save(new User("Aluno Um", "aluno@escapa.com", "hash", "STUDENT")).getId().toString();

        final AdminCourseController controller = new AdminCourseController(
                new GetCourseRulesUseCase(courseRepository, prerequisiteRepository, changeLogRepository),
                new UpdateProgressRulesUseCase(courseRepository, changeLogRepository),
                new AddCoursePrerequisiteUseCase(courseRepository, prerequisiteRepository, changeLogRepository),
                new RemoveCoursePrerequisiteUseCase(courseRepository, prerequisiteRepository, changeLogRepository),
                new SearchCoursesForPrerequisiteUseCase(courseRepository),
                new GetCourseChangeLogUseCase(changeLogRepository),
                new GetAdminCourseUseCase(courseRepository),
                new CreateCourseUseCase(courseRepository, userRepository),
                new PublishCourseUseCase(courseRepository, changeLogRepository, new InMemoryCourseNotificationPort()),
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository),
                new ListAdminCoursesUseCase(courseRepository),
                new ListCourseCategoriesUseCase(courseRepository),
                new ArchiveCourseUseCase(courseRepository),
                new AdminRequestGuard(new AuthorizeAdminUseCase(userRepository))
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldRejectRequestsWithoutTheUserHeader() throws Exception {
        mockMvc.perform(get(COURSES_URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Missing X-User-Id header"));
    }

    @Test
    void shouldRejectNonAdminUsers() throws Exception {
        mockMvc.perform(get(COURSES_URL).header(USER_HEADER, studentId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: user is not ADMIN"));
    }

    @Test
    void shouldListNonArchivedCoursesSortedByTitle() throws Exception {
        saveCourse("beta", CourseStatus.DRAFT);
        saveCourse("Alfa", CourseStatus.PUBLISHED);
        saveCourse("Gama", CourseStatus.ARCHIVED);

        mockMvc.perform(get(COURSES_URL).header(USER_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("Alfa"))
                .andExpect(jsonPath("$.data[1].title").value("beta"));
    }

    @Test
    void shouldCreateADraftCourse() throws Exception {
        mockMvc.perform(post(COURSES_URL).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"  Novo curso  \", \"price\": 10.5, \"durationTime\": 60}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Course created successfully"))
                .andExpect(jsonPath("$.data.title").value("Novo curso"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void shouldRejectInvalidCourseBodyWith400() throws Exception {
        mockMvc.perform(post(COURSES_URL).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\", \"price\": -1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNegativeValuesOnUpdate() throws Exception {
        final UUID courseId = saveCourse("Curso", CourseStatus.DRAFT);

        mockMvc.perform(put(COURSES_URL + "/{id}", courseId).header(USER_HEADER, adminId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"durationTime\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("durationTime must not be negative"));
    }

    @Test
    void shouldArchiveACourse() throws Exception {
        final UUID courseId = saveCourse("Curso", CourseStatus.DRAFT);

        mockMvc.perform(delete(COURSES_URL + "/{id}", courseId).header(USER_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Course archived successfully"));

        mockMvc.perform(get(COURSES_URL).header(USER_HEADER, adminId))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldReturnRulesWithoutTheEnvelope() throws Exception {
        final UUID courseId = saveCourse("Curso", CourseStatus.DRAFT);

        mockMvc.perform(get(COURSES_URL + "/{id}/rules", courseId).header(USER_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.requireSequentialProgress").value(true))
                .andExpect(jsonPath("$.prerequisites.length()").value(0));
    }

    @Test
    void shouldRejectInvalidChangeLogPagination() throws Exception {
        final UUID courseId = saveCourse("Curso", CourseStatus.DRAFT);

        mockMvc.perform(get(COURSES_URL + "/{id}/change-log", courseId).param("page", "-1")
                        .header(USER_HEADER, adminId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid pagination"));
        mockMvc.perform(get(COURSES_URL + "/{id}/change-log", courseId).param("size", "101")
                        .header(USER_HEADER, adminId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnAPageOfTheChangeLogWithDefaults() throws Exception {
        final UUID courseId = saveCourse("Curso", CourseStatus.PUBLISHED);
        changeLogRepository.save(courseId, null, "Primeira alteração.", 1, 0);

        mockMvc.perform(get(COURSES_URL + "/{id}/change-log", courseId).header(USER_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].version").value("1.0"));
    }

    @Test
    void shouldRejectPrerequisiteSearchWithoutQuery() throws Exception {
        final UUID courseId = saveCourse("Curso", CourseStatus.DRAFT);

        mockMvc.perform(get(COURSES_URL + "/{id}/prerequisites/search", courseId).header(USER_HEADER, adminId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'query'"));
    }

    @Test
    void shouldSearchPrerequisitesByTitleExcludingTheCourseItself() throws Exception {
        final UUID courseId = saveCourse("Gestão hoteleira", CourseStatus.DRAFT);
        saveCourse("Gestão de eventos", CourseStatus.PUBLISHED);

        mockMvc.perform(get(COURSES_URL + "/{id}/prerequisites/search", courseId).param("query", "gestão")
                        .header(USER_HEADER, adminId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Gestão de eventos"));
    }

    private UUID saveCourse(String title, CourseStatus status) {
        final Course course = new Course();
        course.setTitle(title);
        course.setStatus(status);
        return courseRepository.save(course).getId();
    }
}
