package com.escapa.backend.application.usecase;

import java.util.UUID;

import com.escapa.backend.application.model.CourseDetails;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.domain.course.CourseNotFoundException;

public class GetCourseDetailsUseCase {

    private final CourseRepositoryPort courseRepositoryPort;

    public GetCourseDetailsUseCase(CourseRepositoryPort courseRepositoryPort) {
        this.courseRepositoryPort = courseRepositoryPort;
    }

    public CourseDetails execute(UUID id) {
        return courseRepositoryPort.findDetailsById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
    }
}