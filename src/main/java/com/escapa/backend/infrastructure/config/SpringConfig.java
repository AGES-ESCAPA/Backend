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
import com.escapa.backend.application.usecase.GetCourseChangeLogUseCase;
import com.escapa.backend.application.usecase.GetCourseRulesUseCase;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.ListUsersUseCase;
import com.escapa.backend.application.usecase.PublishCourseUseCase;
import com.escapa.backend.application.usecase.RemoveCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.SearchCoursesForPrerequisiteUseCase;
import com.escapa.backend.application.usecase.UpdateCourseUseCase;
import com.escapa.backend.application.usecase.UpdateProgressRulesUseCase;
import com.escapa.backend.infrastructure.persistence.NotificationJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserCourseJpaRepository;
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
            CourseRepositoryPort courseRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        return new UpdateProgressRulesUseCase(courseRepository, changeLogRepository);
    }

    @Bean
    public AddCoursePrerequisiteUseCase addCoursePrerequisiteUseCase(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        return new AddCoursePrerequisiteUseCase(
                courseRepository, prerequisiteRepository, changeLogRepository);
    }

    @Bean
    public SearchCoursesForPrerequisiteUseCase searchCoursesForPrerequisiteUseCase(
            CourseRepositoryPort courseRepository) {
        return new SearchCoursesForPrerequisiteUseCase(courseRepository);
    }

    @Bean
    public RemoveCoursePrerequisiteUseCase removeCoursePrerequisiteUseCase(
            CourseRepositoryPort courseRepository,
            CoursePrerequisiteRepositoryPort prerequisiteRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        return new RemoveCoursePrerequisiteUseCase(
                courseRepository, prerequisiteRepository, changeLogRepository);
    }

    @Bean
    public GetCourseChangeLogUseCase getCourseChangeLogUseCase(
            CourseChangeLogRepositoryPort changeLogRepository) {
        return new GetCourseChangeLogUseCase(changeLogRepository);
    }

    @Bean
    public PublishCourseUseCase publishCourseUseCase(
            CourseRepositoryPort courseRepository,
            CourseChangeLogRepositoryPort changeLogRepository,
            UserCourseJpaRepository userCourseRepository,
            NotificationJpaRepository notificationRepository) {
        return new PublishCourseUseCase(
                courseRepository, changeLogRepository, userCourseRepository, notificationRepository);
    }

    @Bean
    public UpdateCourseUseCase updateCourseUseCase(
            CourseRepositoryPort courseRepository,
            CourseChangeLogRepositoryPort changeLogRepository) {
        return new UpdateCourseUseCase(courseRepository, changeLogRepository);
    }
}
