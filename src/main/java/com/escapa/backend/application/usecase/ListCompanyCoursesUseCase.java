package com.escapa.backend.application.usecase;
import com.escapa.backend.application.dto.CompanyCourseSummary;
import com.escapa.backend.application.port.CompanyCourseRepositoryPort;
import java.util.List;
import java.util.UUID;

public class ListCompanyCoursesUseCase {
    private final CompanyCourseRepositoryPort companyCourseRepositoryPort;

    public ListCompanyCoursesUseCase(CompanyCourseRepositoryPort companyCourseRepositoryPort) {
        this.companyCourseRepositoryPort = companyCourseRepositoryPort;
    }

    public List<CompanyCourseSummary> execute(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("CompanyId cannot be null");
        }

        return companyCourseRepositoryPort.findActiveCourses(companyId);
    }
}
