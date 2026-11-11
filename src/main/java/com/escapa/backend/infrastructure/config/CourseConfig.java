package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;
import com.escapa.backend.application.port.CourseNotificationPort;
import com.escapa.backend.application.port.CoursePrerequisiteRepositoryPort;
import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.CurriculumReleasePort;
import com.escapa.backend.application.port.LessonNotificationPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.ArchiveCourseUseCase;
import com.escapa.backend.application.usecase.CreateCourseUseCase;
import com.escapa.backend.application.usecase.GetAdminCourseUseCase;
import com.escapa.backend.application.usecase.ListAdminCoursesUseCase;
import com.escapa.backend.application.usecase.ListCourseCategoriesUseCase;
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
            CoursePrerequisiteRepositoryPort prerequisiteRepositoryPort) {
        return new UpdateCourseUseCase(courseRepositoryPort, userRepositoryPort, prerequisiteRepositoryPort);
    }

    @Bean
    public PublishCourseUseCase publishCourseUseCase(
            CourseRepositoryPort courseRepositoryPort,
            CourseChangeLogRepositoryPort changeLogRepositoryPort,
            CourseNotificationPort courseNotificationPort,
            UserRepositoryPort userRepositoryPort,
            CoursePrerequisiteRepositoryPort prerequisiteRepositoryPort,
            CurriculumReleasePort curriculumReleasePort,
            LessonNotificationPort lessonNotificationPort) {
        return new PublishCourseUseCase(
                courseRepositoryPort, changeLogRepositoryPort, courseNotificationPort,
                userRepositoryPort, prerequisiteRepositoryPort, curriculumReleasePort, lessonNotificationPort);
    }

    @Bean
    public ArchiveCourseUseCase archiveCourseUseCase(CourseRepositoryPort courseRepositoryPort) {
        return new ArchiveCourseUseCase(courseRepositoryPort);
    }

    @Bean
    public ListAdminCoursesUseCase listAdminCoursesUseCase(CourseRepositoryPort courseRepositoryPort) {
        return new ListAdminCoursesUseCase(courseRepositoryPort);
    }

    @Bean
    public ListCourseCategoriesUseCase listCourseCategoriesUseCase(CourseRepositoryPort courseRepositoryPort) {
        return new ListCourseCategoriesUseCase(courseRepositoryPort);
    }

    @Bean
    public GetAdminCourseUseCase getAdminCourseUseCase(CourseRepositoryPort courseRepositoryPort) {
        return new GetAdminCourseUseCase(courseRepositoryPort);
    }
}
