package com.escapa.backend.common;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para testes de repository.
 *
 * <p>Sobe só a fatia JPA (entidades, repositórios, Flyway) contra o Postgres real,
 * sem controllers nem services. Cada teste roda dentro de uma transação que é
 * desfeita ao final, então não precisa limpar tabela.
 *
 * <p>Como as migrations do Flyway criam o schema, o Hibernate fica em {@code validate}
 * (herdado de application.properties) e nunca recria tabelas nem apaga triggers.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class JpaIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = PostgresTestContainer.INSTANCE;
}
