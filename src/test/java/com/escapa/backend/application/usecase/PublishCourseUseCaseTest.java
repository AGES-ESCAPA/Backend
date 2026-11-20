package com.escapa.backend.application.usecase;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.course.CourseValidationException;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.Module;
import com.escapa.backend.domain.entity.User;
import com.escapa.backend.domain.user.UserNotFoundException;
import com.escapa.backend.application.port.CurriculumReleasePort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublishCourseUseCaseTest {

    @Test
    void shouldPublishWhenAllRequiredFieldsAndContentArePresent() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final InMemoryCourseNotificationPort notificationPort = new InMemoryCourseNotificationPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(completeCourse(admin));
        final PublishCourseUseCase useCase =
                new PublishCourseUseCase(courseRepository, changeLogRepository, notificationPort,
                new InMemoryUserRepositoryPort(), new InMemoryCoursePrerequisiteRepositoryPort());

        final Course published = useCase.execute(course.getId(), false, admin);

        assertEquals(CourseStatus.PUBLISHED, published.getStatus());
        assertEquals(1, published.getMajorVersion());
        assertEquals(0, published.getMinorVersion());
        assertEquals(1, changeLogRepository.entriesFor(course.getId()).size());
        assertTrue(notificationPort.notifiedCourseIds().isEmpty());
    }

    @Test
    void shouldNotifyEnrolledStudentsWhenRequested() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final InMemoryCourseNotificationPort notificationPort = new InMemoryCourseNotificationPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(completeCourse(admin));
        final PublishCourseUseCase useCase =
                new PublishCourseUseCase(courseRepository, changeLogRepository, notificationPort,
                new InMemoryUserRepositoryPort(), new InMemoryCoursePrerequisiteRepositoryPort());

        useCase.execute(course.getId(), true, admin);

        assertEquals(List.of(course.getId()), notificationPort.notifiedCourseIds());
    }

    @Test
    void shouldThrowWithMissingFieldsWhenIncomplete() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final InMemoryCourseNotificationPort notificationPort = new InMemoryCourseNotificationPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        final PublishCourseUseCase useCase =
                new PublishCourseUseCase(courseRepository, changeLogRepository, notificationPort,
                new InMemoryUserRepositoryPort(), new InMemoryCoursePrerequisiteRepositoryPort());

        final CourseValidationException ex = assertThrows(
                CourseValidationException.class, () -> useCase.execute(course.getId(), false, admin));

        assertTrue(ex.getMissingFields().contains("description"));
        assertTrue(ex.getMissingFields().contains("instructorId"));
        assertTrue(ex.getMissingFields().stream().anyMatch(field -> field.startsWith("modules")));
    }

    @Test
    void shouldApplyTheDraftToThePublishedCourseOnlyWhenPublishing() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final InMemoryFileStoragePort fileStorage = new InMemoryFileStoragePort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(completeCourse(admin));
        course.setStatus(CourseStatus.PUBLISHED);
        course.setMajorVersion(1);
        course.setThumbnailUrl("https://storage.test/courses/thumbnails/antiga.png");
        final Course prerequisite = courseRepository.save(new Course(
                "Pre requisito", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        course.setPendingDraft(CoursePendingDraft.fromPublished(course, List.of(prerequisite.getId()))
                .applyUpdate("Titulo publicado depois", null, null,
                        "https://storage.test/courses/thumbnails/nova.png",
                        null, null, null, null, null, null, null, null, null, null));
        courseRepository.save(course);
        final PublishCourseUseCase useCase = new PublishCourseUseCase(
                courseRepository, new InMemoryCourseChangeLogRepositoryPort(), new InMemoryCourseNotificationPort(),
                new InMemoryUserRepositoryPort(), prerequisiteRepository,
                courseId -> List.of(), (lessonId, lessonTitle) -> { }, fileStorage);

        assertEquals("Fundamentos de Design", courseRepository.findById(course.getId()).orElseThrow().getTitle());

        final Course published = useCase.execute(course.getId(), false, admin);

        assertEquals("Titulo publicado depois", published.getTitle());
        assertNull(published.getPendingDraft());
        assertEquals(2, published.getMajorVersion());
        assertTrue(prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(
                course.getId(), prerequisite.getId()));
        assertEquals(List.of("https://storage.test/courses/thumbnails/antiga.png"), fileStorage.getDeletedUrls());
    }

    @Test
    void shouldKeepThePublishedCourseWhenTheDraftCannotBePublished() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(completeCourse(admin));
        course.setStatus(CourseStatus.PUBLISHED);
        course.setMajorVersion(1);
        course.setPendingDraft(CoursePendingDraft.fromPublished(course, List.of())
                .applyUpdate("Titulo incompleto", null, "", null, null, null, null, null,
                        null, null, null, null, null, null));
        courseRepository.save(course);
        final PublishCourseUseCase useCase = new PublishCourseUseCase(
                courseRepository, new InMemoryCourseChangeLogRepositoryPort(), new InMemoryCourseNotificationPort(),
                new InMemoryUserRepositoryPort(), new InMemoryCoursePrerequisiteRepositoryPort());

        assertThrows(CourseValidationException.class, () -> useCase.execute(course.getId(), false, admin));

        final Course stored = courseRepository.findById(course.getId()).orElseThrow();
        assertEquals("Fundamentos de Design", stored.getTitle());
        assertEquals("Descricao completa", stored.getDescription());
        assertEquals(CourseStatus.PUBLISHED, stored.getStatus());
        assertEquals(1, stored.getMajorVersion());
        assertEquals("Titulo incompleto", stored.getPendingDraft().title());
    }

    @Test
    void shouldRejectPublishingWhenDraftPrerequisitesFormACycle() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisiteRepository =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course courseA = courseRepository.save(completeCourse(admin));
        courseA.setTitle("Curso A");
        courseA.setStatus(CourseStatus.PUBLISHED);
        courseA.setMajorVersion(1);
        final Course courseB = courseRepository.save(completeCourse(admin));
        courseB.setTitle("Curso B");
        courseB.setStatus(CourseStatus.PUBLISHED);
        courseB.setMajorVersion(1);
        courseRepository.save(courseA);
        courseRepository.save(courseB);
        // Rascunhos cruzados: A exige B e B exige A.
        courseA.setPendingDraft(CoursePendingDraft.fromPublished(courseA, List.of(courseB.getId())));
        courseB.setPendingDraft(CoursePendingDraft.fromPublished(courseB, List.of(courseA.getId())));
        courseRepository.save(courseA);
        courseRepository.save(courseB);
        final PublishCourseUseCase useCase = new PublishCourseUseCase(
                courseRepository, new InMemoryCourseChangeLogRepositoryPort(), new InMemoryCourseNotificationPort(),
                new InMemoryUserRepositoryPort(), prerequisiteRepository);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(courseA.getId(), false, admin));
        assertTrue(prerequisiteRepository.findPrerequisiteCourseIds(courseA.getId()).isEmpty());
    }

    @Test
    void shouldPublishWithSameThumbnailAndNotifyReleasedLessons() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCourseChangeLogRepositoryPort changeLogRepository = new InMemoryCourseChangeLogRepositoryPort();
        final InMemoryCourseNotificationPort courseNotifications = new InMemoryCourseNotificationPort();
        final InMemoryFileStoragePort fileStorage = new InMemoryFileStoragePort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(completeCourse(admin));
        final String thumbnail = "https://storage.test/courses/thumbnails/current.png";
        course.setStatus(CourseStatus.PUBLISHED);
        course.setMajorVersion(1);
        course.setThumbnailUrl(thumbnail);
        course.setPendingDraft(CoursePendingDraft.fromPublished(course, List.of())
                .applyUpdate("Novo título", null, null, thumbnail, null, null,
                        null, null, null, null, null, null, null, null));
        courseRepository.save(course);
        final UUID lessonId = UUID.randomUUID();
        final CurriculumReleasePort releasePort = courseId -> List.of(
                new CurriculumReleasePort.ReleasedLessonChange(lessonId, "Aula atualizada"));
        final List<CurriculumReleasePort.ReleasedLessonChange> notifiedLessons = new ArrayList<>();
        final PublishCourseUseCase useCase = new PublishCourseUseCase(
                courseRepository, changeLogRepository, courseNotifications, new InMemoryUserRepositoryPort(),
                new InMemoryCoursePrerequisiteRepositoryPort(), releasePort,
                (id, title) -> notifiedLessons.add(new CurriculumReleasePort.ReleasedLessonChange(id, title)),
                fileStorage);

        final Course published = useCase.execute(course.getId(), false, null);

        assertEquals("Novo título", published.getTitle());
        assertTrue(fileStorage.getDeletedUrls().isEmpty());
        assertEquals(List.of(new CurriculumReleasePort.ReleasedLessonChange(lessonId, "Aula atualizada")),
                notifiedLessons);
        assertEquals(2, published.getMajorVersion());
        assertTrue(courseNotifications.notifiedCourseIds().isEmpty());
    }

    @Test
    void shouldResolveChangedDraftInstructorAndRejectUnknownInstructor() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryUserRepositoryPort users = new InMemoryUserRepositoryPort();
        final User originalInstructor = users.save(new User("Original", "original@example.com", "hash", "ADMIN"));
        final User nextInstructor = users.save(new User("Nova", "nova@example.com", "hash", "ADMIN"));
        final Course course = courseRepository.save(completeCourse(originalInstructor));
        course.setStatus(CourseStatus.PUBLISHED);
        course.setMajorVersion(1);
        course.setPendingDraft(CoursePendingDraft.fromPublished(course, List.of())
                .applyUpdate(null, null, null, null, null, nextInstructor.getId(),
                        null, null, null, null, null, null, null, null));
        courseRepository.save(course);
        final PublishCourseUseCase useCase = new PublishCourseUseCase(
                courseRepository, new InMemoryCourseChangeLogRepositoryPort(), new InMemoryCourseNotificationPort(),
                users, new InMemoryCoursePrerequisiteRepositoryPort());

        final Course published = useCase.execute(course.getId(), false, originalInstructor);

        assertEquals(nextInstructor.getId(), published.getInstructor().getId());
        final Course invalidCourse = completeCourse(originalInstructor);
        invalidCourse.setStatus(CourseStatus.PUBLISHED);
        invalidCourse.setMajorVersion(1);
        invalidCourse.setPendingDraft(CoursePendingDraft.fromPublished(invalidCourse, List.of())
                .applyUpdate(null, null, null, null, null, UUID.randomUUID(),
                        null, null, null, null, null, null, null, null));
        courseRepository.save(invalidCourse);
        assertThrows(UserNotFoundException.class, () -> useCase.execute(
                invalidCourse.getId(), false, originalInstructor));
    }

    @Test
    void shouldReportBlankRequiredFieldsAndEmptyModuleContents() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final Course course = new Course(
                " ", " ", " ", null, null, null, " ", " ",
                null, null, null, List.of(), true, false, null);
        final Module module = new Module(UUID.randomUUID(), course, "Módulo", 1);
        module.setContents(null);
        course.setModules(List.of(module));
        courseRepository.save(course);
        final PublishCourseUseCase useCase = new PublishCourseUseCase(
                courseRepository, new InMemoryCourseChangeLogRepositoryPort(), new InMemoryCourseNotificationPort(),
                new InMemoryUserRepositoryPort(), new InMemoryCoursePrerequisiteRepositoryPort());

        final CourseValidationException exception = assertThrows(
                CourseValidationException.class, () -> useCase.execute(course.getId(), false, null));

        assertTrue(exception.getMissingFields().containsAll(List.of(
                "title", "description", "shortDescription", "instructorId", "category",
                "level", "price", "durationTime", "deadline",
                "modules (at least one module with content is required)")));
    }

    @Test
    void shouldPublishDraftWithNullPrerequisiteList() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final InMemoryCoursePrerequisiteRepositoryPort prerequisites =
                new InMemoryCoursePrerequisiteRepositoryPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(completeCourse(admin));
        course.setStatus(CourseStatus.PUBLISHED);
        course.setMajorVersion(1);
        course.setPendingDraft(new CoursePendingDraft(
                "Fundamentos de Design", "Resumo curto", "Descricao completa", null, null,
                admin.getId(), "Design", "INICIANTE", 40, 365, 499.00,
                List.of("Objetivo 1"), true, false, null));
        courseRepository.save(course);
        final PublishCourseUseCase useCase = new PublishCourseUseCase(
                courseRepository, new InMemoryCourseChangeLogRepositoryPort(),
                new InMemoryCourseNotificationPort(), new InMemoryUserRepositoryPort(), prerequisites);

        final Course published = useCase.execute(course.getId(), false, admin);

        assertEquals(CourseStatus.PUBLISHED, published.getStatus());
        assertEquals(2, published.getMajorVersion());
        assertTrue(prerequisites.findPrerequisiteCourseIds(course.getId()).isEmpty());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final PublishCourseUseCase useCase = new PublishCourseUseCase(
                courseRepository, new InMemoryCourseChangeLogRepositoryPort(), new InMemoryCourseNotificationPort(),
                new InMemoryUserRepositoryPort(), new InMemoryCoursePrerequisiteRepositoryPort());

        assertThrows(CourseNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), false, null));
    }

    private static Course completeCourse(User instructor) {
        final Course course = new Course(
                "Fundamentos de Design", "Resumo curto", "Descricao completa", null, null,
                instructor, "Design", "INICIANTE", 40, 365, 499.00,
                List.of("Objetivo 1"), true, false, instructor);

        final Module module = new Module(UUID.randomUUID(), course, "Modulo 1", 1);
        final Content content = new Content();
        content.setId(UUID.randomUUID());
        content.setTitle("Aula 1");
        module.getContents().add(content);
        course.getModules().add(module);

        return course;
    }
}
