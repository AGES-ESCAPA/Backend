package com.escapa.backend.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.domain.course.CourseNotFoundException;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

class ArchiveCourseUseCaseTest {

    @Test
    void shouldSoftDeleteBySettingStatusToArchived() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        final ArchiveCourseUseCase useCase = new ArchiveCourseUseCase(courseRepository);

        final Course archived = useCase.execute(course.getId());

        assertEquals(CourseStatus.ARCHIVED, archived.getStatus());
        assertEquals(course.getId(), courseRepository.findById(course.getId()).orElseThrow().getId());
    }

    @Test
    void shouldDiscardPendingDraftWhenArchiving() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final User admin = new User("Admin Um", "admin@escapa.com", "hash", "ADMIN");
        admin.setId(UUID.randomUUID());
        final Course course = courseRepository.save(new Course(
                "Titulo", null, null, null, null, null, null, null,
                null, null, null, List.of(), true, false, admin));
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPendingDraft(CoursePendingDraft.fromPublished(course, List.of())
                .applyUpdate("Titulo no rascunho", null, null, null, null, null, null, null,
                        null, null, null, null, null, null));
        courseRepository.save(course);
        final ArchiveCourseUseCase useCase = new ArchiveCourseUseCase(courseRepository);

        final Course archived = useCase.execute(course.getId());

        assertEquals(CourseStatus.ARCHIVED, archived.getStatus());
        assertNull(archived.getPendingDraft());
        assertNull(courseRepository.findById(course.getId()).orElseThrow().getPendingDraft());
    }

    @Test
    void shouldThrowWhenCourseNotFound() {
        final InMemoryCourseRepositoryPort courseRepository = new InMemoryCourseRepositoryPort();
        final ArchiveCourseUseCase useCase = new ArchiveCourseUseCase(courseRepository);

        assertThrows(CourseNotFoundException.class, () -> useCase.execute(UUID.randomUUID()));
    }
}
