package com.escapa.backend.application.usecase;

import com.escapa.backend.adapters.dto.ChangeLogPageResponse;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.infrastructure.persistence.entity.CourseChangeLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

public class GetCourseChangeLogUseCase {
    private final CourseChangeLogRepositoryPort changeLogRepository;

    public GetCourseChangeLogUseCase(CourseChangeLogRepositoryPort changeLogRepository) {
        this.changeLogRepository = changeLogRepository;
    }

    public ChangeLogPageResponse execute(UUID courseId, int page, int size) {
        final Page<CourseChangeLogEntity> result = changeLogRepository.findPageByCourseId(
                courseId, PageRequest.of(page, size));
        return new ChangeLogPageResponse(
                result.getContent().stream().map(log -> new ChangeLogPageResponse.Entry(
                        log.getId(),
                        log.getDescription(),
                        log.getChangedBy() == null ? "Sistema" : log.getChangedBy().getName(),
                        log.getMajorVersion() + "." + log.getMinorVersion(),
                        log.getCreatedAt().toString()
                )).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
