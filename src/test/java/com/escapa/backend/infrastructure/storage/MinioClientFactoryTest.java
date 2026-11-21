package com.escapa.backend.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinioClientFactoryTest {

    private final List<String> authorizations = Collections.synchronizedList(new ArrayList<>());
    private final List<String> requests = Collections.synchronizedList(new ArrayList<>());
    private HttpServer server;
    private String endpoint;

    @BeforeEach
    void startFakeStorage() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::respondToStorageRequest);
        server.start();
        endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void respondToStorageRequest(HttpExchange exchange) throws IOException {
        authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
        exchange.getRequestBody().readAllBytes();
        if (isBucketLocationRequest(exchange)) {
            respondWithBucketRegion(exchange);
        } else {
            exchange.getResponseHeaders().add("ETag", "\"etag\"");
            exchange.sendResponseHeaders(200, -1);
        }
        exchange.close();
    }

    private static boolean isBucketLocationRequest(HttpExchange exchange) {
        final String query = exchange.getRequestURI().getRawQuery();
        return query != null && query.contains("location");
    }

    private static void respondWithBucketRegion(HttpExchange exchange) throws IOException {
        final byte[] response = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<LocationConstraint xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">us-east-1"
                + "</LocationConstraint>").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/xml");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
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
        assertTrue(requests.get(0).startsWith("PUT "));
        assertTrue(authorizations.get(0).contains("/garage/s3/aws4_request"));
        assertFalse(authorizations.get(0).contains("us-east-1"));
    }

    @Test
    void shouldNotFixRegionWhenItIsBlank() throws Exception {
        putObject(MinioClientFactory.create(endpoint, "key", "secret", ""));

        assertTrue(requests.get(0).contains("location"));
        assertTrue(requests.stream().anyMatch(request -> request.startsWith("PUT ")));
        assertTrue(authorizations.get(0).contains("/us-east-1/s3/aws4_request"));
    }
}
