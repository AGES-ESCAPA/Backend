package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.ArchiveCourseUseCase;
import com.escapa.backend.application.usecase.CreateCourseUseCase;
import com.escapa.backend.application.usecase.PublishCourseUseCase;
import com.escapa.backend.application.usecase.UpdateCourseUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CourseConfig {

    @Bean
    public CreateCourseUseCase createCourseUseCase(CourseRepositoryPort courseRepositoryPort, UserRepositoryPort userRepositoryPort) {
        return new CreateCourseUseCase(courseRepositoryPort, userRepositoryPort);
    }

    @Bean
    public UpdateCourseUseCase updateCourseUseCase(CourseRepositoryPort courseRepositoryPort, UserRepositoryPort userRepositoryPort) {
        return new UpdateCourseUseCase(courseRepositoryPort, userRepositoryPort);
    }

    @Bean
    public PublishCourseUseCase publishCourseUseCase(CourseRepositoryPort courseRepositoryPort) {
        return new PublishCourseUseCase(courseRepositoryPort);
    }

    @Bean
    public ArchiveCourseUseCase archiveCourseUseCase(CourseRepositoryPort courseRepositoryPort) {
        return new ArchiveCourseUseCase(courseRepositoryPort);
    }
}
