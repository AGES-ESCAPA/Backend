package com.escapa.backend.common;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Um único Postgres para toda a suíte. Iniciado uma vez, na primeira classe que
 * o referenciar, e reaproveitado por todas as bases de teste ({@link JpaIntegrationTest},
 * {@link WebIntegrationTest}). Não usa {@code @Container} de propósito: isso criaria
 * um container por classe de teste.
 */
public final class PostgresTestContainer {

    public static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        INSTANCE.start();
    }

    private PostgresTestContainer() {
    }
}
