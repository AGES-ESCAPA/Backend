package com.escapa.backend.infrastructure.config;

import com.escapa.backend.infrastructure.persistence.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O Prometheus coleta as metricas na porta de gestao, sem token. A porta da API
 * (publicada pelo Ingress) nao pode expor esse endpoint.
 *
 * <p>{@code @AutoConfigureObservability}: o Spring Boot Test desliga a exportacao de
 * metricas por padrao; sem ele o endpoint nao existe no teste.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
@ActiveProfiles("test")
class MetricsEndpointTest extends PostgresIntegrationTest {

    private static final String METRICS_PATH = "/actuator/prometheus";

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    @Test
    void shouldExposeHttpLatencyHistogramOnManagementPortWithoutToken() {
        restTemplate.getForEntity(url(serverPort, "/api/v1/health"), String.class);

        final ResponseEntity<String> response =
                restTemplate.getForEntity(url(managementPort, METRICS_PATH), String.class);

        assertEquals(200, response.getStatusCode().value());
        final String body = response.getBody();
        assertTrue(body.contains("http_server_requests_seconds_bucket"));
        assertTrue(body.contains("uri=\"/api/v1/health\""));
        assertTrue(body.contains("application=\"escapa-backend\""));
    }

    @Test
    void shouldNotExposeMetricsOnPublicApiPort() {
        assertNotEquals(serverPort, managementPort);

        final ResponseEntity<String> response =
                restTemplate.getForEntity(url(serverPort, METRICS_PATH), String.class);

        assertFalse(response.getStatusCode().is2xxSuccessful());
        final String body = response.getBody();
        assertFalse(body != null && body.contains("http_server_requests"));
    }

    private static String url(final int port, final String path) {
        return "http://localhost:" + port + path;
    }
}
