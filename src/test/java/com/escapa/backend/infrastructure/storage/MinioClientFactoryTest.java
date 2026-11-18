package com.escapa.backend.infrastructure.storage;

import com.sun.net.httpserver.HttpServer;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinioClientFactoryTest {

    private final List<String> authorizations = Collections.synchronizedList(new ArrayList<>());
    private HttpServer server;
    private String endpoint;

    @BeforeEach
    void startFakeStorage() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("ETag", "\"etag\"");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopFakeStorage() {
        server.stop(0);
    }

    private void putObject(MinioClient client) throws Exception {
        client.putObject(PutObjectArgs.builder()
                .bucket("media.escapaages.online")
                .object("courses/a.png")
                .stream(new ByteArrayInputStream(new byte[]{1}), 1L, -1L)
                .contentType("image/png")
                .build());
    }

    @Test
    void shouldSignRequestsWithConfiguredRegion() throws Exception {
        putObject(MinioClientFactory.create(endpoint, "key", "secret", "garage"));

        assertEquals(1, authorizations.size());
        assertTrue(authorizations.get(0).contains("/garage/s3/aws4_request"));
        assertFalse(authorizations.get(0).contains("us-east-1"));
    }

    @Test
    void shouldNotFixRegionWhenItIsBlank() throws Exception {
        try {
            putObject(MinioClientFactory.create(endpoint, "key", "secret", ""));
        } catch (Exception ignored) {
            // sem regiao, o client primeiro pergunta a localizacao do bucket; a resposta do fake nao a traz
        }

        assertFalse(authorizations.isEmpty());
        assertTrue(authorizations.get(0).contains("/us-east-1/s3/aws4_request"));
    }
}
