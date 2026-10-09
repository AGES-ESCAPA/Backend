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

    public static final int MAX_PAGE_SIZE = 100;

    public PageResult<ChangeLogEntry> execute(UUID courseId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid pagination");
        }
        return changeLogRepository.findPageByCourseId(courseId, page, size);
    }
}
