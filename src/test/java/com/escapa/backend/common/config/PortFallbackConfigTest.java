package com.escapa.backend.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ocupa uma porta de verdade com um socket e confere que a configuração desvia para a próxima livre.
 * Não sobe contexto Spring: a classe é chamada direto.
 */
class PortFallbackConfigTest {

    @Test
    void keepsConfiguredPortWhenItIsFree() throws IOException {
        final int free = reserveAndRelease();

        final PortFallbackConfig config = new PortFallbackConfig(free, List.of(free + 1));

        assertEquals(free, config.choosePort());
    }

    @Test
    void fallsBackToFirstFreeCandidateWhenConfiguredPortIsBusy() throws IOException {
        try (ServerSocket busy = new ServerSocket(0)) {
            final int busyPort = busy.getLocalPort();
            final int alternative = reserveAndRelease();

            final PortFallbackConfig config = new PortFallbackConfig(busyPort, List.of(alternative));

            assertEquals(alternative, config.choosePort());
        }
    }

    @Test
    void skipsBusyCandidatesAndTakesTheNextOne() throws IOException {
        try (ServerSocket busy = new ServerSocket(0); ServerSocket busyCandidate = new ServerSocket(0)) {
            final int alternative = reserveAndRelease();

            final PortFallbackConfig config = new PortFallbackConfig(
                    busy.getLocalPort(), List.of(busyCandidate.getLocalPort(), alternative));

            assertEquals(alternative, config.choosePort());
        }
    }

    @Test
    void returnsConfiguredPortWhenEverythingIsBusySoStartupFailsLoudly() throws IOException {
        try (ServerSocket busy = new ServerSocket(0); ServerSocket busyCandidate = new ServerSocket(0)) {
            final PortFallbackConfig config = new PortFallbackConfig(
                    busy.getLocalPort(), List.of(busyCandidate.getLocalPort()));

            assertEquals(busy.getLocalPort(), config.choosePort());
        }
    }

    @Test
    void appliesTheChosenPortToTheServerFactory() throws IOException {
        try (ServerSocket busy = new ServerSocket(0)) {
            final int alternative = reserveAndRelease();
            final TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();

            new PortFallbackConfig(busy.getLocalPort(), List.of(alternative)).customize(factory);

            assertEquals(alternative, factory.getPort());
        }
    }

    /** Pede uma porta livre ao sistema e a devolve fechada, para o teste usá-la como candidata. */
    private static int reserveAndRelease() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
