package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.application.usecase.GetAdminOrderUseCase;
import com.escapa.backend.application.usecase.ListAdminOrdersUseCase;
import com.escapa.backend.infrastructure.persistence.CompanyJpaRepository;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.OrderJpaRepository;
import com.escapa.backend.infrastructure.persistence.OrderRepositoryAdapter;
import com.escapa.backend.infrastructure.persistence.RegularUserJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderConfig {

    @Bean
    public OrderRepositoryPort orderRepositoryPort(
            OrderJpaRepository orderJpaRepository,
            RegularUserJpaRepository regularUserJpaRepository,
            CompanyJpaRepository companyJpaRepository,
            UserJpaRepository userJpaRepository,
            CourseJpaRepository courseJpaRepository
    ) {
        return new OrderRepositoryAdapter(
                orderJpaRepository,
                regularUserJpaRepository,
                companyJpaRepository,
                userJpaRepository,
                courseJpaRepository
        );
    }

    @Bean
    public ListAdminOrdersUseCase listAdminOrdersUseCase(OrderRepositoryPort orderRepositoryPort) {
        return new ListAdminOrdersUseCase(orderRepositoryPort);
    }

    @Bean
    public GetAdminOrderUseCase getAdminOrderUseCase(OrderRepositoryPort orderRepositoryPort) {
        return new GetAdminOrderUseCase(orderRepositoryPort);
    }
}

