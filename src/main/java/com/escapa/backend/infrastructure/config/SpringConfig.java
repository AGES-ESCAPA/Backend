package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.UserRepositoryPort;
import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.ListUsersUseCase;
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
    public CreateUserUseCase createUserUseCase(UserRepositoryPort userRepositoryPort) {
        return new CreateUserUseCase(userRepositoryPort);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserRepositoryPort userRepositoryPort) {
        return new ListUsersUseCase(userRepositoryPort);
    }

    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserRepositoryPort userRepositoryPort) {
        return new GetUserByIdUseCase(userRepositoryPort);
    }
}
