package com.escapa.backend.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;

/**
 * Se a porta configurada ({@code server.port}) já estiver ocupada na máquina, sobe na
 * primeira porta livre de {@code app.server.port-fallback.candidates}.
 *
 * <p>Ligado só no perfil {@code dev} ({@code app.server.port-fallback.enabled=true}).
 * Em homologação e produção a porta é fixa de propósito: proxy, frontend e monitoramento
 * dependem de um endereço previsível, e porta ocupada lá é incidente, não inconveniente.
 *
 * <p>Quando o fallback acontece, o log avisa em WARN e o {@link StartupInfoLogger}
 * imprime a URL real. O frontend precisa apontar para a porta escolhida.
 */
@Configuration
@ConditionalOnProperty(name = "app.server.port-fallback.enabled", havingValue = "true")
public class PortFallbackConfig implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    private static final Logger LOG = LoggerFactory.getLogger(PortFallbackConfig.class);

    private final int configuredPort;
    private final List<Integer> candidates;

    public PortFallbackConfig(
            @Value("${server.port}") int configuredPort,
            @Value("${app.server.port-fallback.candidates}") List<Integer> candidates) {
        this.configuredPort = configuredPort;
        this.candidates = candidates;
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.setPort(choosePort());
    }

    int choosePort() {
        if (isFree(configuredPort)) {
            return configuredPort;
        }
        for (final int candidate : candidates) {
            if (isFree(candidate)) {
                LOG.warn("Porta {} ocupada por outro processo. Subindo na {} (fallback do perfil dev). "
                        + "Aponte o frontend para a porta nova.", configuredPort, candidate);
                return candidate;
            }
        }
        LOG.error("Porta {} e todas as alternativas {} estao ocupadas. A subida vai falhar.",
                configuredPort, candidates);
        return configuredPort;
    }

    static boolean isFree(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress("0.0.0.0", port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
