package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.usecase.ListPublishedCoursesUseCase;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.course.CourseRepositoryAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fiação manual remanescente da camada antiga. Some no passo 4 do refactor,
 * quando o catálogo de cursos virar service anotado.
 */
@Configuration
public class SpringConfig {

    @Bean
    public CourseRepositoryPort courseRepositoryPort(CourseJpaRepository courseJpaRepository) {
        return new CourseRepositoryAdapter(courseJpaRepository);
    }

    @Bean
    public ListPublishedCoursesUseCase listPublishedCoursesUseCase(CourseRepositoryPort courseRepositoryPort) {
        return new ListPublishedCoursesUseCase(courseRepositoryPort);
    }
}
