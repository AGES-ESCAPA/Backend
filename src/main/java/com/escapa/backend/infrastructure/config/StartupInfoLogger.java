package com.escapa.backend.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Loga, ao fim do startup, a URL em que a aplicacao esta respondendo.
 * A porta real e lida do servidor embarcado, entao funciona mesmo com
 * server.port=0 ou com a porta sobrescrita por variavel de ambiente.
 */
@Component
public class StartupInfoLogger {

    private static final Logger LOG = LoggerFactory.getLogger(StartupInfoLogger.class);

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        if (!(event.getApplicationContext() instanceof WebServerApplicationContext webContext)) {
            return;
        }

        final Environment env = event.getApplicationContext().getEnvironment();
        final int port = webContext.getWebServer().getPort();
        final String contextPath = env.getProperty("server.servlet.context-path", "");
        final String baseUrl = "http://localhost:" + port + contextPath;
        final String profiles = String.join(", ", env.getActiveProfiles());

        LOG.info("""

                ----------------------------------------------------------
                  Escapa! backend rodando
                  Local:    {}
                  Swagger:  {}/swagger-ui/index.html
                  Perfis:   {}
                ----------------------------------------------------------
                """, baseUrl, baseUrl, profiles.isEmpty() ? "default" : profiles);
    }
}
