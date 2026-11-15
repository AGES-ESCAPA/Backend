package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.CompanyCourseSummary;
import com.escapa.backend.application.model.AcquiredCourse;
import com.escapa.backend.application.port.CompanyCourseRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Cursos que a empresa possui, em memória. */
public final class FakeAcquiredCourses implements CompanyCourseRepositoryPort {

    private final List<AcquiredCourse> courses = new ArrayList<>();

    void add(UUID courseId, Integer deadline) {
        courses.add(new AcquiredCourse(courseId, deadline));
    }

    @Override
    public List<AcquiredCourse> findAcquiredCourses(UUID companyId) {
        return List.copyOf(courses);
    }

    @Override
    public List<UUID> findAcquiredCourseIds(UUID companyId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CompanyCourseSummary> findActiveCourses(UUID companyId) {
        throw new UnsupportedOperationException();
    }
}
