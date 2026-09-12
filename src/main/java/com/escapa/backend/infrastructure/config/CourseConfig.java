package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CourseNotificationPort;
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
    public CreateCourseUseCase createCourseUseCase(
            CourseRepositoryPort courseRepositoryPort, UserRepositoryPort userRepositoryPort) {
        return new CreateCourseUseCase(courseRepositoryPort, userRepositoryPort);
    }

    @Bean
    public UpdateCourseUseCase updateCourseUseCase(
            CourseRepositoryPort courseRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            CourseChangeLogRepositoryPort changeLogRepositoryPort) {
        return new UpdateCourseUseCase(courseRepositoryPort, userRepositoryPort, changeLogRepositoryPort);
    }

    @Bean
    public PublishCourseUseCase publishCourseUseCase(
            CourseRepositoryPort courseRepositoryPort,
            CourseChangeLogRepositoryPort changeLogRepositoryPort,
            CourseNotificationPort courseNotificationPort) {
        return new PublishCourseUseCase(courseRepositoryPort, changeLogRepositoryPort, courseNotificationPort);
    }

    @Bean
    public ArchiveCourseUseCase archiveCourseUseCase(CourseRepositoryPort courseRepositoryPort) {
        return new ArchiveCourseUseCase(courseRepositoryPort);
    }
}
