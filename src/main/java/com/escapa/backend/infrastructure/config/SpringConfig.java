package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CourseNotificationPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.AddCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.CreateContentUseCase;
import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.DeleteContentUseCase;
import com.escapa.backend.application.usecase.GetContentUseCase;
import com.escapa.backend.application.usecase.GetCourseChangeLogUseCase;
import com.escapa.backend.application.usecase.GetCourseDetailsUseCase;
import com.escapa.backend.application.usecase.GetCourseRulesUseCase;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.ListModuleContentsUseCase;
import com.escapa.backend.application.usecase.ListPublishedCoursesUseCase;
import com.escapa.backend.application.usecase.ListUsersUseCase;
import com.escapa.backend.application.usecase.RemoveCoursePrerequisiteUseCase;
import com.escapa.backend.application.usecase.ReorderContentsUseCase;
import com.escapa.backend.application.usecase.SearchCoursesForPrerequisiteUseCase;
import com.escapa.backend.application.usecase.UpdateContentUseCase;
import com.escapa.backend.application.usecase.UpdateProgressRulesUseCase;
import com.escapa.backend.infrastructure.persistence.ContentJpaRepository;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserRepositoryAdapter;
import com.escapa.backend.infrastructure.persistence.course.CourseRepositoryAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public CourseRepositoryPort courseRepositoryPort(
            CourseJpaRepository courseJpaRepository,
            ContentJpaRepository contentJpaRepository,
            UserJpaRepository userJpaRepository,
            EntityManager entityManager,
            ObjectMapper objectMapper
    ) {
        return new CourseRepositoryAdapter(
                courseJpaRepository, contentJpaRepository, userJpaRepository, entityManager, objectMapper);
    }

    @Bean
    public ListPublishedCoursesUseCase listPublishedCoursesUseCase(CourseRepositoryPort courseRepositoryPort) {
        return new ListPublishedCoursesUseCase(courseRepositoryPort);
    }

    @Bean
    public GetCourseDetailsUseCase getCourseDetailsUseCase(
            CourseRepositoryPort courseRepositoryPort
    ) {
        return new GetCourseDetailsUseCase(courseRepositoryPort);
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
    public CreateContentUseCase createContentUseCase(
            ContentRepositoryPort contentRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort
    ) {
        return new CreateContentUseCase(contentRepositoryPort, moduleRepositoryPort);
    }

    @Bean
    public UpdateContentUseCase updateContentUseCase(ContentRepositoryPort contentRepositoryPort) {
        return new UpdateContentUseCase(contentRepositoryPort);
    }

    @Bean
    public GetContentUseCase getContentUseCase(
            ContentRepositoryPort contentRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort
    ) {
        return new GetContentUseCase(contentRepositoryPort, moduleRepositoryPort);
    }

    @Bean
    public ListModuleContentsUseCase listModuleContentsUseCase(
            ContentRepositoryPort contentRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort
    ) {
        return new ListModuleContentsUseCase(contentRepositoryPort, moduleRepositoryPort);
    }

    @Bean
    public DeleteContentUseCase deleteContentUseCase(ContentRepositoryPort contentRepositoryPort) {
        return new DeleteContentUseCase(contentRepositoryPort);
    }

    @Bean
    public ReorderContentsUseCase reorderContentsUseCase(
            ContentRepositoryPort contentRepositoryPort,
            ModuleRepositoryPort moduleRepositoryPort
    ) {
        return new ReorderContentsUseCase(contentRepositoryPort, moduleRepositoryPort);
    }
}
