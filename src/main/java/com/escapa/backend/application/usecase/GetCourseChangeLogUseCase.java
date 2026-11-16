package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;

import java.util.UUID;

public class GetCourseChangeLogUseCase {

    private final CourseChangeLogRepositoryPort changeLogRepository;

    public GetCourseChangeLogUseCase(CourseChangeLogRepositoryPort changeLogRepository) {
        this.changeLogRepository = changeLogRepository;
    }

    public PageResult<ChangeLogEntry> execute(UUID courseId, int page, int size) {
        PaginationRules.requireValid(page, size);
        return changeLogRepository.findPageByCourseId(courseId, page, size);
    }
}
