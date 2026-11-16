package com.escapa.backend.infrastructure.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Envia pelo adaptador de verdade para um Mailpit em container (a mesma imagem do
 * docker-compose) e confere, pela API do Mailpit, o que chegou na caixa de entrada.
 */
class MailpitEmailIntegrationTest {

    private static final int SMTP_PORT = 1025;
    private static final int HTTP_PORT = 8025;
    private static final String FROM = "nao-responda@escapa.com.br";
    private static final String TO = "ana@email.com";

    private static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:v1.31.0")
            .withExposedPorts(SMTP_PORT, HTTP_PORT)
            .waitingFor(Wait.forHttp("/api/v1/info").forPort(HTTP_PORT));

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    static void startMailpit() {
        MAILPIT.start();
    }

    @AfterAll
    static void stopMailpit() {
        MAILPIT.stop();
    }

    @Test
    void shouldDeliverTheInviteToTheMailpitInboxMarkedAsTest() throws Exception {
        final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(MAILPIT.getHost());
        mailSender.setPort(MAILPIT.getMappedPort(SMTP_PORT));
        mailSender.setDefaultEncoding("UTF-8");
        final SmtpEmailSenderAdapter sender = new SmtpEmailSenderAdapter(mailSender, FROM, true, "");
        final ResourceEmailTemplateAdapter templates = new ResourceEmailTemplateAdapter();
        final Map<String, String> variables = Map.of(
                "nome", "Ana", "empresa", "Pousada Vista Mar", "link", "http://localhost:3000/convite?token=abc");

        sender.send(TO, "Convite para a Escapa!",
                templates.render("convite-colaborador.html", variables),
                templates.render("convite-colaborador.txt", variables));

        final JsonNode summary = get("/api/v1/messages").path("messages").get(0);
        assertEquals(FROM, summary.path("From").path("Address").asText());
        assertEquals(TO, summary.path("To").get(0).path("Address").asText());
        assertEquals("[TESTE] Convite para a Escapa!", summary.path("Subject").asText());

        final JsonNode message = get("/api/v1/message/" + summary.path("ID").asText());
        final String html = message.path("HTML").asText();
        assertTrue(html.contains(SmtpEmailSenderAdapter.TEST_NOTICE));
        assertTrue(html.contains("Pousada Vista Mar"));
        assertTrue(message.path("Text").asText().startsWith(SmtpEmailSenderAdapter.TEST_NOTICE));
    }

    private JsonNode get(String path) throws Exception {
        final URI uri = URI.create("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(HTTP_PORT) + path);
        final HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return json.readTree(response.body());
    }
}
