package com.escapa.backend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.AddCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.GetCourseRulesUseCase;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.ListUsersUseCase;
import com.escapa.backend.application.usecase.UpdateProgressRulesUseCase;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserRepositoryAdapter;

@Configuration
public class SpringConfig {

    @Bean
    public UserRepositoryPort userRepositoryPort(UserJpaRepository userJpaRepository) {
        return new UserRepositoryAdapter(userJpaRepository);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(
            UserRepositoryPort userRepositoryPort,
            PasswordHasherPort passwordHasherPort
    ) {
        return new CreateUserUseCase(userRepositoryPort, passwordHasherPort);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepositoryPort userRepositoryPort) {
        return new ListUsersUseCase(userRepositoryPort);
    }

    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserRepositoryPort userRepositoryPort) {
        return new GetUserByIdUseCase(userRepositoryPort);
    }

    @Bean
    public GetCourseRulesUseCase getCourseRulesUseCase(
            CourseRepositoryPort courseRepository,
            CourseChangeLogRepositoryPort courseChangeLogRepository,
            CoursePrerequisiteRepositoryPort coursePrerequisiteRepository
    ) {
        return new GetCourseRulesUseCase(
            courseRepository,
            coursePrerequisiteRepository,
            courseChangeLogRepository
        );
    }

    @Bean
    public UpdateProgressRulesUseCase updateProgressRulesUseCase(
            CourseRepositoryPort courseRepository) {
        return new UpdateProgressRulesUseCase(courseRepository);
    }

    @Bean
    public AddCoursePrerequisiteUseCase addCoursePrerequisiteUseCase(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository) {
        return new AddCoursePrerequisiteUseCase(courseRepository, prerequisiteRepository);
    }
}
