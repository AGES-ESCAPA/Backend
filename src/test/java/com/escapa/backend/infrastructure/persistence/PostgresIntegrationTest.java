package com.escapa.backend.infrastructure.persistence;

import com.escapa.backend.common.PostgresTestContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
abstract class PostgresIntegrationTest {

    // Reaproveita o container único da suíte (ver common.PostgresTestContainer).
    // Esta base some no passo 4 do refactor, quando os testes antigos forem apagados.
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = PostgresTestContainer.INSTANCE;
}
