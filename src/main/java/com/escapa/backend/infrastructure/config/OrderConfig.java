package com.escapa.backend.infrastructure.config;

import com.escapa.backend.application.port.CourseRepositoryPort;
import com.escapa.backend.application.port.OrderAccessPort;
import com.escapa.backend.application.port.OrderRepositoryPort;
import com.escapa.backend.application.usecase.CreateOrderUseCase;
import com.escapa.backend.infrastructure.persistence.OrderAccessAdapter;
import com.escapa.backend.infrastructure.persistence.UserCourseJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;

/** Beans da criação de pedidos (US-27). */
@Configuration
public class OrderConfig {

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
