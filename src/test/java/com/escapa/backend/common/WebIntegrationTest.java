package com.escapa.backend.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para testes de controller.
 *
 * <p>Sobe o contexto completo com MockMvc contra o Postgres real. Usar para validar
 * o contrato HTTP de ponta a ponta: rota, status, envelope e nomes dos campos do JSON.
 * Regra de negócio se testa no service com Mockito, não aqui.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class WebIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = PostgresTestContainer.INSTANCE;

    @Autowired
    protected MockMvc mockMvc;
}
