package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.ContentRepositoryPort;
import com.escapa.backend.application.port.ModuleRepositoryPort;
import com.escapa.backend.application.port.PasswordHasherPort;
import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.CreateContentUseCase;
import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.DeleteContentUseCase;
import com.escapa.backend.application.usecase.GetContentUseCase;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.ListUsersUseCase;
import com.escapa.backend.application.usecase.ReorderContentsUseCase;
import com.escapa.backend.application.usecase.UpdateContentUseCase;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserRepositoryAdapter;
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
