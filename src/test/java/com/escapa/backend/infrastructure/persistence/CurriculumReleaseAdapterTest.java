package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.application.port.CurriculumReleasePort;
import com.escapa.backend.domain.content.ContentPendingDraft;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.infrastructure.persistence.entity.ContentEntity;
import com.escapa.backend.infrastructure.persistence.entity.CourseEntity;
import com.escapa.backend.infrastructure.persistence.entity.ModuleEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Transactional
class CurriculumReleaseAdapterTest extends PostgresIntegrationTest {

    @Autowired
    private CurriculumReleasePort curriculumReleasePort;

    @Autowired
    private CourseJpaRepository courseJpaRepository;

    @Autowired
    private ModuleJpaRepository moduleJpaRepository;

    @Autowired
    private ContentJpaRepository contentJpaRepository;

    @Test
    void shouldPublishModuleAndLessonDraftsAndDeletePendingItems() {
        final CourseEntity course = givenCourse();
        givenModule(course, 1, true, "Deleted module");
        final ModuleEntity activeModule = givenModule(course, 2, false, "Module before");
        activeModule.setPendingTitle("Module after");
        activeModule.setContents(new ArrayList<>(List.of(
                givenContent(activeModule, 1, "Deleted lesson", true, null, false),
                givenContent(activeModule, 2, "Lesson before", false, draft("Lesson after"), true),
                givenContent(activeModule, 3, "Stable lesson", false, draft("Stable lesson"), true),
                givenContent(activeModule, 4, "New lesson", false, draft("New lesson"), false))));
        moduleJpaRepository.save(activeModule);
        final ContentEntity changedLesson = activeModule.getContents().get(1);
        contentJpaRepository.saveAll(activeModule.getContents());

        final List<CurriculumReleasePort.ReleasedLessonChange> changes =
                curriculumReleasePort.release(course.getId());

        assertEquals(List.of(new CurriculumReleasePort.ReleasedLessonChange(
                changedLesson.getId(), "Lesson after")), changes);
        final List<ModuleEntity> modules = moduleJpaRepository.findByCourseIdOrderByOrderAsc(course.getId());
        assertEquals(1, modules.size());
        assertEquals("Module after", modules.get(0).getTitle());
        assertNull(modules.get(0).getPendingTitle());
        assertEquals(2, modules.get(0).getPublishedOrder());
        final List<ContentEntity> contents = contentJpaRepository
                .findByModuleIdOrderByOrderAsc(modules.get(0).getId());
        assertEquals(3, contents.size());
        assertEquals("Lesson after", contents.get(0).getTitle());
        assertEquals(2, contents.get(0).getPublishedOrder());
        assertNull(contents.get(0).getPendingDraft());
        assertEquals("Stable lesson", contents.get(1).getTitle());
        assertEquals("New lesson", contents.get(2).getTitle());
        assertEquals(4, contents.get(2).getPublishedOrder());
    }

    @Test
    void shouldReleaseEmptyCourseWithoutChanges() {
        final CourseEntity course = givenCourse();

        assertEquals(List.of(), curriculumReleasePort.release(course.getId()));
    }

    private CourseEntity givenCourse() {
        final CourseEntity course = new CourseEntity();
        course.setTitle("Curso publicado");
        return courseJpaRepository.save(course);
    }

    private ModuleEntity givenModule(CourseEntity course, int order, boolean pendingDeletion, String title) {
        final ModuleEntity module = new ModuleEntity();
        module.setCourse(course);
        module.setOrder(order);
        module.setTitle(title);
        module.setPendingDeletion(pendingDeletion);
        module.setContents(new ArrayList<>());
        return moduleJpaRepository.save(module);
    }

    private ContentEntity givenContent(
            ModuleEntity module, int order, String title, boolean pendingDeletion,
            ContentPendingDraft pendingDraft, boolean published) {
        final ContentEntity content = new ContentEntity();
        content.setModule(module);
        content.setOrder(order);
        content.setTitle(title);
        content.setDescription("Description");
        content.setType(ContentType.VIDEO);
        content.setUrl("https://example.com/video.mp4");
        content.setDurationMinutes(10);
        content.setIsFree(false);
        content.setPendingDeletion(pendingDeletion);
        content.setPendingDraft(pendingDraft == null ? null : ContentMapper.writePendingDraft(pendingDraft));
        content.setPublishedOrder(published ? order : null);
        return contentJpaRepository.save(content);
    }

    private ContentPendingDraft draft(String title) {
        return new ContentPendingDraft(
                title, "Description", ContentType.VIDEO, "https://example.com/video.mp4",
                10, false, List.of());
    }
}
