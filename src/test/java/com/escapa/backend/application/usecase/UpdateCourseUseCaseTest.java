package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCourseUseCaseTest {

    @ParameterizedTest(name = "published course changes {0}")
    @MethodSource("publishedCourseChanges")
    void shouldBumpVersionForEveryPublishedCourseFieldChange(String field, CourseUpdate update) {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository =
                new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin", "admin@example.com", "hash", "ADMIN"));
        final User instructor = userRepository.save(new User("Instructor", "instructor@example.com",
                "hash", "ADMIN"));
        final Course course = publishedCourse(admin);
        courseRepository.save(course);
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), update.title(), update.shortDescription(), update.description(),
                update.thumbnailUrl(), update.teaserVideoUrl(), update.instructorId(instructor),
                update.category(), update.level(), update.durationTime(), update.deadline(), update.price(),
                update.learningObjectives(), update.requireSequentialProgress(), update.enforceDeadlineBlock(), admin);

        assertEquals(5, updated.getMinorVersion(), field);
        assertEquals(1, changeLogRepository.entriesFor(course.getId()).size(), field);
    }

    @ParameterizedTest(name = "published course ignores blank or absent title: {0}")
    @MethodSource("unchangedTitles")
    void shouldNotVersionPublishedCourseWhenTitleDoesNotChange(String title) {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository =
                new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin", "admin@example.com", "hash", "ADMIN"));
        final Course course = publishedCourse(admin);
        courseRepository.save(course);
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), title, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);

        assertEquals("Original", updated.getTitle());
        assertEquals(4, updated.getMinorVersion());
        assertTrue(changeLogRepository.entriesFor(course.getId()).isEmpty());
    }

    @Test
    void shouldVersionPublishedCourseWithSystemAsChangeAuthorWhenAuthorIsMissing() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository =
                new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin", "admin@example.com", "hash", "ADMIN"));
        final Course course = publishedCourse(admin);
        courseRepository.save(course);
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), "Changed", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);

        assertEquals(5, updated.getMinorVersion());
        assertEquals("Sistema", changeLogRepository.entriesFor(course.getId()).get(0).changedByName());
    }

    @Test
    void shouldPartiallyUpdateFieldsWithoutTouchingCreatedBy() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo original", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), "Titulo novo", null, null, null, null, null, null,
                null, null, null, null, null, null, null, admin);

        assertEquals("Titulo novo", updated.getTitle());
        assertEquals(admin.getId(), updated.getCreatedBy().getId());
    }

    @Test
    void shouldReplaceInstructor() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final User newInstructor = userRepository.save(new User("Instrutora Nova", "nova@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), null, null, null, null, null, newInstructor.getId(), null,
                null, null, null, null, null, null, null, admin);

        assertEquals(newInstructor.getId(), updated.getInstructor().getId());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final UpdateCourseUseCase useCase = new UpdateCourseUseCase(
                courseRepository, userRepository, new InMemoryCourseChangeLogRepositoryPort());

        assertThrows(CourseNotFoundException.class, () -> useCase.execute(
                UUID.randomUUID(), "Titulo", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null));
    }

    @Test
    void shouldThrowWhenInstructorIsNotAdmin() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final User student = userRepository.save(new User("Aluno Um", "aluno@escapa.com", "hash", "STUDENT"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(
                course.getId(), null, null, null, null, null, student.getId(), null,
                null, null, null, null, null, null, null, admin));
    }

    @Test
    void shouldBumpMinorVersionAndLogWhenPublishedCourseChanges() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        course.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(course);
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), "Titulo revisado", null, null, null, null, null, null,
                null, null, null, null, null, null, null, admin);

        assertEquals(1, updated.getMinorVersion());
        assertEquals(1, changeLogRepository.entriesFor(course.getId()).size());
    }

    @Test
    void shouldNotBumpVersionWhenDraftCourseChanges() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort userRepository = new InMemoryUserRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final User admin = userRepository.save(new User("Admin Um", "admin@escapa.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        final UpdateCourseUseCase useCase =
                new UpdateCourseUseCase(courseRepository, userRepository, changeLogRepository);

        final Course updated = useCase.execute(
                course.getId(), "Titulo revisado", null, null, null, null, null, null,
                null, null, null, null, null, null, null, admin);

        assertEquals(0, updated.getMinorVersion());
        assertTrue(changeLogRepository.entriesFor(course.getId()).isEmpty());
    }

    private static Stream<Arguments> publishedCourseChanges() {
        return Stream.of(
                Arguments.of("title", new CourseUpdate("Updated", null, null, null, null, null,
                        null, null, null, null, null, null, null, null)),
                Arguments.of("shortDescription", new CourseUpdate(null, "New summary", null, null, null, null,
                        null, null, null, null, null, null, null, null)),
                Arguments.of("description", new CourseUpdate(null, null, "New description", null, null, null,
                        null, null, null, null, null, null, null, null)),
                Arguments.of("thumbnailUrl", new CourseUpdate(null, null, null, "/new.png", null, null,
                        null, null, null, null, null, null, null, null)),
                Arguments.of("teaserVideoUrl", new CourseUpdate(null, null, null, null, "/new.mp4", null,
                        null, null, null, null, null, null, null, null)),
                Arguments.of("instructor", new CourseUpdate(null, null, null, null, null, true,
                        null, null, null, null, null, null, null, null)),
                Arguments.of("category", new CourseUpdate(null, null, null, null, null, null,
                        "New category", null, null, null, null, null, null, null)),
                Arguments.of("level", new CourseUpdate(null, null, null, null, null, null,
                        null, "Advanced", null, null, null, null, null, null)),
                Arguments.of("durationTime", new CourseUpdate(null, null, null, null, null, null,
                        null, null, 11, null, null, null, null, null)),
                Arguments.of("deadline", new CourseUpdate(null, null, null, null, null, null,
                        null, null, null, 21, null, null, null, null)),
                Arguments.of("price", new CourseUpdate(null, null, null, null, null, null,
                        null, null, null, null, 11.0, null, null, null)),
                Arguments.of("learningObjectives", new CourseUpdate(null, null, null, null, null, null,
                        null, null, null, null, null, List.of("New objective"), null, null)),
                Arguments.of("requireSequentialProgress", new CourseUpdate(null, null, null, null, null, null,
                        null, null, null, null, null, null, false, null)),
                Arguments.of("enforceDeadlineBlock", new CourseUpdate(null, null, null, null, null, null,
                        null, null, null, null, null, null, null, true))
        );
    }

    private static Stream<Arguments> unchangedTitles() {
        return Stream.of(Arguments.of((String) null), Arguments.of(" "), Arguments.of("  \t  "));
    }

    private static Course publishedCourse(User admin) {
        final Course course = new Course("Original", "Summary", "Description", "/thumb.png", "/teaser.mp4",
                admin, "Tourism", "Beginner", 10, 20, 10.0, List.of("Objective"),
                true, false, admin);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setMajorVersion(2);
        course.setMinorVersion(4);
        return course;
    }

    private record CourseUpdate(
            String title,
            String shortDescription,
            String description,
            String thumbnailUrl,
            String teaserVideoUrl,
            Boolean changesInstructor,
            String category,
            String level,
            Integer durationTime,
            Integer deadline,
            Double price,
            List<String> learningObjectives,
            Boolean requireSequentialProgress,
            Boolean enforceDeadlineBlock) {

        private UUID instructorId(User instructor) {
            return Boolean.TRUE.equals(changesInstructor) ? instructor.getId() : null;
        }
    }
}
