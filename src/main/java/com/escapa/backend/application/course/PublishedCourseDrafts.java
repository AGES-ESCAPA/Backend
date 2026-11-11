package com.escapa.backend.application.course;

import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.domain.course.CoursePendingDraft;
import com.escapa.backend.domain.course.CourseStatus;
import com.escapa.backend.domain.entity.Course;
import com.escapa.backend.domain.entity.User;

import java.util.UUID;

/**
 * Curso publicado e o rascunho ainda não publicado convivem no mesmo registro:
 * os alunos leem as colunas publicadas; o construtor lê o rascunho por cima.
 */
public final class PublishedCourseDrafts {

    private PublishedCourseDrafts() {
    }

    public static boolean isPublished(Course course) {
        return course.getStatus() == CourseStatus.PUBLISHED;
    }

    public static CoursePendingDraft ensure(Course course, CoursePrerequisiteRepositoryPort prerequisites) {
        if (course.getPendingDraft() != null) {
            return course.getPendingDraft();
        }
        return CoursePendingDraft.fromPublished(
                course, prerequisites.findPrerequisiteCourseIds(course.getId()));
    }

    /**
     * Visão do construtor. Não altera o curso armazenado: a cópia recebe os campos
     * do rascunho e a versão publicada continua sendo a que os alunos veem.
     */
    public static Course editorView(Course course) {
        if (!isPublished(course) || course.getPendingDraft() == null) {
            return course;
        }

        final Course view = new Course();
        view.setId(course.getId());
        view.setStatus(course.getStatus());
        view.setCreatedBy(course.getCreatedBy());
        view.setCreatedAt(course.getCreatedAt());
        view.setUpdatedAt(course.getUpdatedAt());
        view.setMajorVersion(course.getMajorVersion());
        view.setMinorVersion(course.getMinorVersion());
        view.setModules(course.getModules());
        course.getPendingDraft().applyTo(view);

        final UUID instructorId = course.getPendingDraft().instructorId();
        if (instructorId == null) {
            return view;
        }
        if (course.getInstructor() != null && instructorId.equals(course.getInstructor().getId())) {
            view.setInstructor(course.getInstructor());
            return view;
        }
        final User instructor = new User();
        instructor.setId(instructorId);
        view.setInstructor(instructor);
        return view;
    }
}
