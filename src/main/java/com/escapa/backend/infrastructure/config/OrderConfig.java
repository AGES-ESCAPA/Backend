package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.OrderAccessPort;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.application.usecase.CreateOrderUseCase;
import com.escapa.backend.application.usecase.GetAdminOrderUseCase;
import com.escapa.backend.application.usecase.ListAdminOrdersUseCase;
import com.escapa.backend.infrastructure.persistence.CompanyJpaRepository;
import com.escapa.backend.infrastructure.persistence.CourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.OrderAccessAdapter;
import com.escapa.backend.infrastructure.persistence.OrderJpaRepository;
import com.escapa.backend.infrastructure.persistence.OrderRepositoryAdapter;
import com.escapa.backend.infrastructure.persistence.RegularUserJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserCourseJpaRepository;
import com.escapa.backend.infrastructure.persistence.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;

/** Beans dos pedidos: listagem e detalhe do admin (US-28) e criação com liberação de acesso (US-27). */
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

    @Bean
    public OrderAccessPort orderAccessPort(UserCourseJpaRepository userCourseRepository, EntityManager entityManager) {
        return new OrderAccessAdapter(userCourseRepository, entityManager);
    }

    /**
     * O valor do assento vem da configuração ({@code APP_SEAT_PRICE}) porque nenhuma US define o
     * modelo comercial do pacote de licenças; quando a cliente definir, muda só a configuração.
     */
    @Bean
    public CreateOrderUseCase createOrderUseCase(
            OrderRepositoryPort orderRepositoryPort,
            CourseRepositoryPort courseRepositoryPort,
            OrderAccessPort orderAccessPort,
            @Value("${app.seat-price}") BigDecimal seatPrice
    ) {
        return new CreateOrderUseCase(
                orderRepositoryPort, courseRepositoryPort, orderAccessPort, seatPrice, Clock.systemDefaultZone());
    }
}
